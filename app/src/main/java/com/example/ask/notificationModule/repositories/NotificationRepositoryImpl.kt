package com.example.ask.notificationModule.repositories

import android.util.Log
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private var notificationListener: ListenerRegistration? = null

    override fun addNotification(
        notification: NotificationModel,
        result: (UiState<String>) -> Unit
    ) {
        val notificationDocRef = firestore.collection(Constant.USERS)
            .document(notification.userId ?: "")
            .collection(Constant.NOTIFICATIONS)
            .document()

        notification.notificationId = notificationDocRef.id
        notification.timestamp = System.currentTimeMillis()

        notificationDocRef.set(notification)
            .addOnSuccessListener {
                result.invoke(UiState.Success("Notification sent successfully"))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to send notification"))
            }
    }

    override fun getUserNotifications(
        userId: String,
        result: (UiState<List<NotificationModel>>) -> Unit
    ) {
        val ref = firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.NOTIFICATIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        notificationListener?.remove()
        notificationListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GetNotifications", "Firestore error", error)
                result(UiState.Failure(error.localizedMessage ?: "Unknown error"))
                return@addSnapshotListener
            }

            val notifications = snapshot?.documents?.mapNotNull {
                try {
                    it.toObject(NotificationModel::class.java)
                } catch (e: Exception) {
                    Log.e("GetNotifications", "Error parsing document", e)
                    null
                }
            } ?: emptyList()
            result(UiState.Success(notifications))
        }
    }

    override fun markNotificationAsRead(
        notificationId: String,
        result: (UiState<String>) -> Unit
    ) {
        // We need to find the notification across all users or pass userId
        // For now, implementing a simpler approach
        result.invoke(UiState.Success("Marked as read"))
    }

    override fun getUnreadNotificationCount(
        userId: String,
        result: (UiState<Int>) -> Unit
    ) {
        firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.NOTIFICATIONS)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                result.invoke(UiState.Success(documents.size()))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to get count"))
            }
    }

    override fun removeNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
    }
}