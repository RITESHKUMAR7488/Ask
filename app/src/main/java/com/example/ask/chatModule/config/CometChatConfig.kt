package com.example.ask.chatModule.config

object CometChatConfig {
    // Replace these with your actual CometChat credentials
    const val APP_ID = "281374075cca11b6"
    const val REGION = "in" // e.g., "us" or "eu"
    const val AUTH_KEY = "41b30117b174f5e9eca9643a48db805e07133670"

    // CometChat User ID will be the same as your Firebase user ID for consistency
    fun getCometChatUserId(firebaseUserId: String): String {
        return firebaseUserId
    }
}