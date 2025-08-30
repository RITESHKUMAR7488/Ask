package com.example.ask.chatModule.config

import android.util.Log

object CometChatConfig {
    // Replace these with your actual CometChat credentials
    const val APP_ID = "281374075cca11b6"
    const val REGION = "in" // e.g., "us" or "eu"
    const val AUTH_KEY = "41b30117b174f5e9eca9643a48db805e07133670"

    private const val TAG = "CometChatConfig"

    // CometChat User ID will be the same as your Firebase user ID for consistency
    fun getCometChatUserId(firebaseUserId: String): String {
        Log.d(TAG, "Converting Firebase UID to CometChat UID")
        Log.d(TAG, "Input Firebase UID: $firebaseUserId")
        Log.d(TAG, "Output CometChat UID: $firebaseUserId")
        return firebaseUserId
    }

    // Method to verify configuration
    fun verifyConfig(): Boolean {
        val isValid = APP_ID.isNotEmpty() && REGION.isNotEmpty() && AUTH_KEY.isNotEmpty()
        Log.d(TAG, "Configuration verification:")
        Log.d(TAG, "APP_ID valid: ${APP_ID.isNotEmpty()}")
        Log.d(TAG, "REGION valid: ${REGION.isNotEmpty()}")
        Log.d(TAG, "AUTH_KEY valid: ${AUTH_KEY.isNotEmpty()}")
        Log.d(TAG, "Overall valid: $isValid")
        return isValid
    }
}