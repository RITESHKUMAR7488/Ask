package com.example.ask.notificationModule.repositories

import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.utilities.UiState
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUserNotifications(userId: String, result: (UiState<List<NotificationModel>>) -> Unit)

    // --- COROUTINE USAGE ---
    // Declaring suspend functions for async operations.
    suspend fun markNotificationAsRead(userId: String, notificationId: String): UiState<Unit>
    suspend fun markAllNotificationsAsRead(userId: String): UiState<Unit>

    fun addNotification(userId: String, notification: NotificationModel, result: (UiState<String>) -> Unit)
}