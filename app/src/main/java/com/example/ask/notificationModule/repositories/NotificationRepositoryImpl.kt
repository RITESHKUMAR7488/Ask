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
                Log.d("AddNotification", "Notification sent successfully to user: ${notification.userId}")
                result.invoke(UiState.Success("Notification sent successfully"))
            }
            .addOnFailureListener { exception ->
                Log.e("AddNotification", "Failed to send notification", exception)
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

            val notifications = snapshot?.documents?.mapNotNull { document ->
                try {
                    val notification = document.toObject(NotificationModel::class.java)
                    notification?.notificationId = document.id
                    notification
                } catch (e: Exception) {
                    Log.e("GetNotifications", "Error parsing document: ${document.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d("GetNotifications", "Retrieved ${notifications.size} notifications for user: $userId")
            result(UiState.Success(notifications))
        }
    }

    override fun markNotificationAsRead(
        notificationId: String,
        result: (UiState<String>) -> Unit
    ) {
        // This requires knowing which user the notification belongs to
        // For a better implementation, you should pass userId as well
        // For now, let's implement a query-based approach

        firestore.collectionGroup(Constant.NOTIFICATIONS)
            .whereEqualTo("notificationId", notificationId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val batch = firestore.batch()
                    for (document in documents) {
                        batch.update(document.reference, "isRead", true)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("MarkAsRead", "Notification marked as read: $notificationId")
                            result.invoke(UiState.Success("Marked as read"))
                        }
                        .addOnFailureListener { exception ->
                            Log.e("MarkAsRead", "Failed to mark as read", exception)
                            result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to mark as read"))
                        }
                } else {
                    result.invoke(UiState.Failure("Notification not found"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MarkAsRead", "Failed to find notification", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to mark as read"))
            }
    }

    // Better version of markNotificationAsRead with userId
    override fun markNotificationAsRead(
        userId: String,
        notificationId: String,
        result: (UiState<String>) -> Unit
    ) {
        firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.NOTIFICATIONS)
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                Log.d("MarkAsRead", "Notification marked as read: $notificationId")
                result.invoke(UiState.Success("Marked as read"))
            }
            .addOnFailureListener { exception ->
                Log.e("MarkAsRead", "Failed to mark as read", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to mark as read"))
            }
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
                val count = documents.size()
                Log.d("UnreadCount", "Unread notifications for user $userId: $count")
                result.invoke(UiState.Success(count))
            }
            .addOnFailureListener { exception ->
                Log.e("UnreadCount", "Failed to get unread count", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to get count"))
            }
    }

    // Additional helper method to delete notification
    override fun deleteNotification(
        userId: String,
        notificationId: String,
        result: (UiState<String>) -> Unit
    ) {
        firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .addOnSuccessListener {
                Log.d("DeleteNotification", "Notification deleted: $notificationId")
                result.invoke(UiState.Success("Notification deleted"))
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteNotification", "Failed to delete notification", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to delete notification"))
            }
    }

    // Helper method to mark all notifications as read
    override fun markAllNotificationsAsRead(
        userId: String,
        result: (UiState<String>) -> Unit
    ) {
        firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.NOTIFICATIONS)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    result.invoke(UiState.Success("No unread notifications"))
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                for (document in documents) {
                    batch.update(document.reference, "isRead", true)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("MarkAllAsRead", "All notifications marked as read for user: $userId")
                        result.invoke(UiState.Success("All notifications marked as read"))
                    }
                    .addOnFailureListener { exception ->
                        Log.e("MarkAllAsRead", "Failed to mark all as read", exception)
                        result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to mark all as read"))
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("MarkAllAsRead", "Failed to get unread notifications", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to mark all as read"))
            }
    }

    override fun removeNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
        Log.d("NotificationRepo", "Notification listener removed")
    }
}