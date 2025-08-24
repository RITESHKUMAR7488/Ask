package com.example.ask.notificationModule.repositories

import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.utilities.UiState

interface NotificationRepository {
    fun addNotification(
        notification: NotificationModel,
        result: (UiState<String>) -> Unit
    )

    fun getUserNotifications(
        userId: String,
        result: (UiState<List<NotificationModel>>) -> Unit
    )

    fun markNotificationAsRead(
        notificationId: String,
        result: (UiState<String>) -> Unit
    )

    fun getUnreadNotificationCount(
        userId: String,
        result: (UiState<Int>) -> Unit
    )

    fun removeNotificationListener()
}