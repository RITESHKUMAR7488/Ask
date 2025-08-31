package com.example.ask.chatModule.repositories

import android.util.Log
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject // IMPORTANT: Using javax.inject, not jakarta.inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
    }

    override fun createOrGetChat(
        queryId: String,
        queryTitle: String,
        participantIds: List<String>,
        participantNames: Map<String, String>,
        participantImages: Map<String, String>,
        result: (UiState<ChatModel>) -> Unit
    ) {
        result.invoke(UiState.Loading)

        // Check if chat already exists for this query with these participants
        firestore.collection(CHATS_COLLECTION)
            .whereEqualTo("queryId", queryId)
            .whereArrayContainsAny("participants", participantIds)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Chat exists, return it
                    val existingChat = documents.documents[0].toObject(ChatModel::class.java)
                    if (existingChat != null) {
                        result.invoke(UiState.Success(existingChat))
                    } else {
                        result.invoke(UiState.Failure("Failed to parse existing chat"))
                    }
                } else {
                    // Create new chat
                    val chatId = firestore.collection(CHATS_COLLECTION).document().id
                    val unreadCount = participantIds.associateWith { 0 }

                    val newChat = ChatModel(
                        chatId = chatId,
                        queryId = queryId,
                        queryTitle = queryTitle,
                        participants = participantIds,
                        participantNames = participantNames,
                        participantImages = participantImages,
                        lastMessage = null,
                        lastMessageTime = null,
                        lastMessageSenderId = null,
                        unreadCount = unreadCount,
                        createdAt = System.currentTimeMillis(),
                        isActive = true
                    )

                    firestore.collection(CHATS_COLLECTION)
                        .document(chatId)
                        .set(newChat)
                        .addOnSuccessListener {
                            Log.d(TAG, "Chat created successfully with ID: $chatId")
                            result.invoke(UiState.Success(newChat))
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to create chat", e)
                            result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to create chat"))
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check existing chat", e)
                result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to check existing chat"))
            }
    }

    override fun getUserChats(userId: String, result: (UiState<List<ChatModel>>) -> Unit) {
        result.invoke(UiState.Loading)

        firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to get user chats", error)
                    result.invoke(UiState.Failure(error.localizedMessage ?: "Failed to get chats"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatModel::class.java)
                    }
                    Log.d(TAG, "Retrieved ${chats.size} chats for user: $userId")
                    result.invoke(UiState.Success(chats))
                } else {
                    result.invoke(UiState.Success(emptyList()))
                }
            }
    }

    override fun sendMessage(
        chatId: String,
        message: MessageModel,
        result: (UiState<String>) -> Unit
    ) {
        result.invoke(UiState.Loading)

        val messageId = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .document().id

        message.messageId = messageId
        message.chatId = chatId
        message.timestamp = System.currentTimeMillis()

        // Add message to messages subcollection
        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .document(messageId)
            .set(message)
            .addOnSuccessListener {
                // Update chat's last message info
                val updates = hashMapOf<String, Any>(
                    "lastMessage" to (message.message ?: ""),
                    "lastMessageTime" to (message.timestamp ?: System.currentTimeMillis()),
                    "lastMessageSenderId" to (message.senderId ?: "")
                )

                // Increment unread count for all participants except sender
                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .get()
                    .addOnSuccessListener { document ->
                        val chat = document.toObject(ChatModel::class.java)
                        if (chat != null) {
                            val newUnreadCount = chat.unreadCount?.toMutableMap() ?: mutableMapOf()
                            chat.participants?.forEach { participantId ->
                                if (participantId != message.senderId) {
                                    newUnreadCount[participantId] = (newUnreadCount[participantId] ?: 0) + 1
                                }
                            }
                            updates["unreadCount"] = newUnreadCount
                        }

                        firestore.collection(CHATS_COLLECTION)
                            .document(chatId)
                            .update(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "Message sent successfully")
                                result.invoke(UiState.Success("Message sent"))
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update chat", e)
                                result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to update chat"))
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message", e)
                result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to send message"))
            }
    }

    override fun getMessages(chatId: String): Flow<List<MessageModel>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to get messages", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(MessageModel::class.java)
                    }
                    Log.d(TAG, "Retrieved ${messages.size} messages for chat: $chatId")
                    trySend(messages)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    override fun markMessagesAsRead(chatId: String, userId: String) {
        // Reset unread count for this user
        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .update("unreadCount.$userId", 0)
            .addOnSuccessListener {
                Log.d(TAG, "Messages marked as read for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to mark messages as read", e)
            }

        // Mark individual messages as read
        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", userId)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    doc.reference.update("isRead", true)
                }
            }
    }

    override fun deleteMessage(
        chatId: String,
        messageId: String,
        result: (UiState<String>) -> Unit
    ) {
        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .document(messageId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Message deleted successfully")
                result.invoke(UiState.Success("Message deleted"))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete message", e)
                result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to delete message"))
            }
    }

    override fun editMessage(
        chatId: String,
        messageId: String,
        newMessage: String,
        result: (UiState<String>) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "message" to newMessage,
            "isEdited" to true,
            "editedAt" to System.currentTimeMillis()
        )

        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .document(messageId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Message edited successfully")
                result.invoke(UiState.Success("Message edited"))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to edit message", e)
                result.invoke(UiState.Failure(e.localizedMessage ?: "Failed to edit message"))
            }
    }
}