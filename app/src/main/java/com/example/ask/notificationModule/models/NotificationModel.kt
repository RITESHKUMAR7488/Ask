package com.example.ask.notificationModule.models

import com.google.firebase.Timestamp

data class NotificationModel(
    // --- THIS IS THE FIX ---
    var notificationId: String? = null, // Changed from 'val' to 'var'
    // -----------------------

    val userId: String? = null,
    val title: String? = null,
    val message: String? = null,
    val type: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long? = null,

    // --- DATA FOR NAVIGATION ---
    val queryId: String? = null,
    val communityId: String? = null,

    // --- SENDER INFO (Used for CHAT and new CONTACT feature) ---
    val senderUserId: String? = null,
    val senderUserName: String? = null,
    val senderProfileImage: String? = null,
    val senderPhone: String? = null
) {
    // Add a no-argument constructor for Firebase deserialization
    constructor() : this(
        notificationId = null,
        userId = null,
        title = null,
        message = null,
        type = null,
        isRead = false,
        timestamp = null,
        queryId = null,
        communityId = null,
        senderUserId = null,
        senderUserName = null,
        senderProfileImage = null,
        senderPhone = null
    )
}