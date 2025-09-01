package com.example.ask.utilities

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object ChatDebugUtility {
    private const val TAG = "ChatDebug"

    /**
     * Debug method to check chat and message data in Firestore
     */
    fun debugChatData(chatId: String, userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "=== DEBUGGING CHAT DATA ===")
        Log.d(TAG, "ChatId: $chatId")
        Log.d(TAG, "UserId: $userId")

        // 1. Check chat document
        firestore.collection("chats").document(chatId).get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    Log.d(TAG, "✅ Chat document exists")
                    Log.d(TAG, "Chat data: ${chatDoc.data}")

                    val participants = chatDoc.get("participants") as? List<*>
                    val unreadCount = chatDoc.get("unreadCount") as? Map<*, *>
                    val lastMessage = chatDoc.getString("lastMessage")
                    val lastMessageTime = chatDoc.getLong("lastMessageTime")

                    Log.d(TAG, "Participants: $participants")
                    Log.d(TAG, "Unread counts: $unreadCount")
                    Log.d(TAG, "Last message: $lastMessage")
                    Log.d(TAG, "Last message time: $lastMessageTime")

                } else {
                    Log.e(TAG, "❌ Chat document does not exist!")
                }

                // 2. Check messages subcollection
                firestore.collection("chats").document(chatId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .get()
                    .addOnSuccessListener { messagesSnapshot ->
                        Log.d(TAG, "=== MESSAGES IN CHAT ===")
                        Log.d(TAG, "Total messages: ${messagesSnapshot.size()}")

                        messagesSnapshot.documents.forEachIndexed { index, messageDoc ->
                            Log.d(TAG, "Message $index:")
                            Log.d(TAG, "  ID: ${messageDoc.id}")
                            Log.d(TAG, "  Data: ${messageDoc.data}")
                            Log.d(TAG, "  Sender: ${messageDoc.getString("senderName")} (${messageDoc.getString("senderId")})")
                            Log.d(TAG, "  Content: ${messageDoc.getString("message")}")
                            Log.d(TAG, "  Timestamp: ${messageDoc.getLong("timestamp")}")
                            Log.d(TAG, "  IsRead: ${messageDoc.getBoolean("isRead")}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to get messages", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to get chat document", e)
            }
    }

    /**
     * Debug method to check user's chats
     */
    fun debugUserChats(userId: String) {
        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "=== DEBUGGING USER CHATS ===")
        Log.d(TAG, "UserId: $userId")

        firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener { chatsSnapshot ->
                Log.d(TAG, "Total chats for user: ${chatsSnapshot.size()}")

                chatsSnapshot.documents.forEachIndexed { index, chatDoc ->
                    Log.d(TAG, "Chat $index:")
                    Log.d(TAG, "  ID: ${chatDoc.id}")
                    Log.d(TAG, "  Query: ${chatDoc.getString("queryTitle")}")
                    Log.d(TAG, "  Participants: ${chatDoc.get("participants")}")
                    Log.d(TAG, "  Last Message: ${chatDoc.getString("lastMessage")}")
                    Log.d(TAG, "  Unread Count: ${chatDoc.get("unreadCount")}")
                    Log.d(TAG, "  IsActive: ${chatDoc.getBoolean("isActive")}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to get user chats", e)
            }
    }

    /**
     * Debug method to verify message sending
     */
    fun debugMessageSending(chatId: String, messageContent: String, senderId: String) {
        Log.d(TAG, "=== DEBUGGING MESSAGE SENDING ===")
        Log.d(TAG, "ChatId: $chatId")
        Log.d(TAG, "Message: $messageContent")
        Log.d(TAG, "Sender: $senderId")
        Log.d(TAG, "Timestamp: ${System.currentTimeMillis()}")

        // Check if message was actually saved
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("message", messageContent)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Found ${snapshot.size()} matching messages")
                if (snapshot.isEmpty) {
                    Log.e(TAG, "❌ Message not found in Firestore!")
                } else {
                    Log.d(TAG, "✅ Message found in Firestore")
                    snapshot.documents.forEach { doc ->
                        Log.d(TAG, "Message data: ${doc.data}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to verify message", e)
            }
    }
}