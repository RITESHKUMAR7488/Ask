package com.example.ask.notificationModule.repositories

import android.util.Log
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val TAG = "NotificationRepositoryImpl"

    override fun getUserNotifications(
        userId: String,
        result: (UiState<List<NotificationModel>>) -> Unit
    ) {
        result.invoke(UiState.Loading)
        firestore.collection(Constant.USERS).document(userId)
            .collection(Constant.NOTIFICATIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for notifications: ${error.message}", error)
                    result.invoke(UiState.Failure(error.localizedMessage))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull {
                        it.toObject(NotificationModel::class.java)?.apply {
                            notificationId = it.id
                        }
                    }
                    Log.d(TAG, "Successfully fetched ${notifications.size} notifications")
                    result.invoke(UiState.Success(notifications))
                }
            }
    }

    // --- COROUTINE USAGE EXPLANATION ---
    // This is a 'suspend' function, which means it can be paused and resumed.
    // It allows us to perform asynchronous operations (like a database write)
    // without blocking the main thread.
    override suspend fun markNotificationAsRead(
        userId: String,
        notificationId: String
    ): UiState<Unit> {
        // 'withContext(Dispatchers.IO)' moves the execution of this block
        // to an IO-optimized thread pool, which is best for network/disk operations.
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection(Constant.USERS)
                    .document(userId)
                    .collection(Constant.NOTIFICATIONS)
                    .document(notificationId)
                    .update("read", true)
                    .await() // '.await()' is a suspend function from kotlinx-coroutines-play-services
                // It waits for the Firestore task to complete.
                Log.d(TAG, "Successfully marked notification $notificationId as read for user $userId")
                UiState.Success(Unit)
            } catch (e: CancellationException) {
                Log.i(TAG, "Mark notification as read cancelled", e)
                throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification $notificationId as read: ${e.message}", e)
                UiState.Failure(e.localizedMessage)
            }
        }
    }

    // --- COROUTINE USAGE EXPLANATION ---
    // This 'suspend' function is similar to the one above.
    override suspend fun markAllNotificationsAsRead(userId: String): UiState<Unit> {
        // 'withContext(Dispatchers.IO)' again ensures this heavy operation
        // doesn't block the UI thread.
        return withContext(Dispatchers.IO) {
            try {
                // We use a write batch to update all documents atomically.
                val writeBatch = firestore.batch()
                val querySnapshot = firestore.collection(Constant.USERS)
                    .document(userId)
                    .collection(Constant.NOTIFICATIONS)
                    .whereEqualTo("read", false)
                    .get()
                    .await() // Wait for the query to finish

                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "No unread notifications to mark as read for user $userId")
                    return@withContext UiState.Success(Unit) // Nothing to update
                }

                Log.d(TAG, "Marking ${querySnapshot.size()} notifications as read for user $userId")
                querySnapshot.documents.forEach { document ->
                    writeBatch.update(document.reference, "read", true)
                }

                writeBatch.commit().await() // Wait for the batch commit to finish
                UiState.Success(Unit)
            } catch (e: CancellationException) {
                Log.i(TAG, "Mark all notifications as read cancelled", e)
                throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all notifications as read: ${e.message}", e)
                UiState.Failure(e.localizedMessage)
            }
        }
    }

    override fun addNotification(
        userId: String,
        notification: NotificationModel,
        result: (UiState<String>)-> Unit
    ) {
        result.invoke(UiState.Loading)
        firestore.collection(Constant.USERS).document(userId)
            .collection(Constant.NOTIFICATIONS)
            .add(notification)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully added notification for user $userId")
                result.invoke(UiState.Success("Notification added successfully"))
            }
            .addOnFailureListener {
                Log.e(TAG, "Error adding notification: ${it.message}", it)
                result.invoke(UiState.Failure(it.localizedMessage))
            }
    }
}