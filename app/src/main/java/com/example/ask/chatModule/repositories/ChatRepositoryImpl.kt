package com.example.ask.chatModule.repositories

import android.util.Log
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.ParticipantInfo
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private var messageListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "ChatRepository"
        private const val CHAT_ROOMS = "chatRooms"
        private const val MESSAGES = "messages"
    }

    override fun createOrGetChatRoom(
        queryId: String,
        queryTitle: String,
        queryOwnerId: String,
        queryOwnerName: String,
        currentUserId: String,
        currentUserName: String,
        result: (UiState<ChatRoomModel>) -> Unit
    ) {
        Log.d(TAG, "createOrGetChatRoom: queryId=$queryId, queryOwner=$queryOwnerId, currentUser=$currentUserId")

        val chatRoomId = generateChatRoomId(queryId, queryOwnerId, currentUserId)
        Log.d(TAG, "Generated chatRoomId: $chatRoomId")

        val chatRoomRef = firestore.collection(CHAT_ROOMS).document(chatRoomId)

        // Check if chat room already exists
        chatRoomRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Chat room exists, returning existing room")
                    val chatRoom = document.toObject(ChatRoomModel::class.java)
                    if (chatRoom != null) {
                        chatRoom.chatRoomId = chatRoomId // Ensure ID is set
                        result.invoke(UiState.Success(chatRoom))
                    } else {
                        result.invoke(UiState.Failure("Failed to parse existing chat room"))
                    }
                } else {
                    Log.d(TAG, "Creating new chat room")
                    createNewChatRoom(
                        chatRoomId, queryId, queryTitle, queryOwnerId,
                        queryOwnerName, currentUserId, currentUserName, result
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check chat room existence", exception)
                result.invoke(UiState.Failure(exception.message ?: "Failed to access chat room"))
            }
    }

    private fun createNewChatRoom(
        chatRoomId: String,
        queryId: String,
        queryTitle: String,
        queryOwnerId: String,
        queryOwnerName: String,
        currentUserId: String,
        currentUserName: String,
        result: (UiState<ChatRoomModel>) -> Unit
    ) {
        val participants = listOf(queryOwnerId, currentUserId).distinct()
        Log.d(TAG, "Creating chat room with participants: $participants")

        val participantDetails = mapOf(
            queryOwnerId to ParticipantInfo(
                userId = queryOwnerId,
                userName = queryOwnerName,
                joinedAt = System.currentTimeMillis()
            ),
            currentUserId to ParticipantInfo(
                userId = currentUserId,
                userName = currentUserName,
                joinedAt = System.currentTimeMillis()
            )
        )

        val chatRoom = ChatRoomModel(
            chatRoomId = chatRoomId,
            queryId = queryId,
            queryTitle = queryTitle,
            queryOwnerId = queryOwnerId,
            queryOwnerName = queryOwnerName,
            participants = participants,
            participantDetails = participantDetails,
            lastMessage = "Chat started",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageSenderId = currentUserId,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        // Create chat room document
        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .set(chatRoom)
            .addOnSuccessListener {
                Log.d(TAG, "Chat room created successfully with ID: $chatRoomId")
                result.invoke(UiState.Success(chatRoom))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to create chat room", exception)
                result.invoke(UiState.Failure(exception.message ?: "Failed to create chat room"))
            }
    }

    override fun sendMessage(
        chatRoomId: String,
        message: ChatModel,
        result: (UiState<String>) -> Unit
    ) {
        Log.d(TAG, "sendMessage: chatRoomId=$chatRoomId, message=${message.message}")

        // Generate message ID if not set
        if (message.messageId.isNullOrEmpty()) {
            message.messageId = firestore.collection(CHAT_ROOMS)
                .document(chatRoomId)
                .collection(MESSAGES)
                .document().id
        }

        // Set timestamp if not set
        if (message.timestamp == 0L) {
            message.timestamp = System.currentTimeMillis()
        }

        // Add message to subcollection
        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .document(message.messageId!!)
            .set(message)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully: ${message.messageId}")

                // Update chat room's last message
                updateChatRoomLastMessage(chatRoomId, message)

                result.invoke(UiState.Success("Message sent"))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to send message", exception)
                result.invoke(UiState.Failure(exception.message ?: "Failed to send message"))
            }
    }

    private fun updateChatRoomLastMessage(chatRoomId: String, message: ChatModel) {
        val updates = mapOf(
            "lastMessage" to message.message,
            "lastMessageTime" to message.timestamp,
            "lastMessageSenderId" to message.senderId
        )

        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Chat room last message updated")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to update chat room last message", exception)
            }
    }

    override fun getMessages(
        chatRoomId: String,
        result: (UiState<List<ChatModel>>) -> Unit
    ) {
        Log.d(TAG, "getMessages: chatRoomId=$chatRoomId")

        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val messages = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(ChatModel::class.java)
                        message?.messageId = doc.id // Ensure message ID is set
                        message
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: ${doc.id}", e)
                        null
                    }
                }
                Log.d(TAG, "Retrieved ${messages.size} messages")
                result.invoke(UiState.Success(messages))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get messages", exception)
                result.invoke(UiState.Failure(exception.message ?: "Failed to load messages"))
            }
    }

    override fun addMessageListener(
        chatRoomId: String,
        callback: (List<ChatModel>) -> Unit
    ) {
        Log.d(TAG, "addMessageListener: chatRoomId=$chatRoomId")

        // Remove existing listener first
        removeMessageListener()

        messageListener = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e(TAG, "Message listener error", exception)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val messages = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val message = doc.toObject(ChatModel::class.java)
                            message?.messageId = doc.id // Ensure message ID is set
                            message
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing message in listener: ${doc.id}", e)
                            null
                        }
                    }
                    Log.d(TAG, "Real-time update: ${messages.size} messages")
                    callback(messages)
                } else {
                    Log.w(TAG, "QuerySnapshot is null in message listener")
                }
            }
    }

    override fun removeMessageListener() {
        messageListener?.remove()
        messageListener = null
        Log.d(TAG, "Message listener removed")
    }

    override fun markMessagesAsRead(
        chatRoomId: String,
        userId: String,
        result: (UiState<String>) -> Unit
    ) {
        Log.d(TAG, "markMessagesAsRead: chatRoomId=$chatRoomId, userId=$userId")

        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "No unread messages to mark")
                    result.invoke(UiState.Success("No messages to mark as read"))
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                var updateCount = 0

                querySnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                    updateCount++
                }

                if (updateCount > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Marked $updateCount messages as read")
                            result.invoke(UiState.Success("Messages marked as read"))
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to commit batch update", exception)
                            result.invoke(UiState.Failure(exception.message ?: "Failed to mark messages as read"))
                        }
                } else {
                    result.invoke(UiState.Success("No messages to mark as read"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to query unread messages", exception)
                result.invoke(UiState.Failure(exception.message ?: "Failed to update read status"))
            }
    }

    private fun generateChatRoomId(queryId: String, userId1: String, userId2: String): String {
        // Sort user IDs to ensure consistent chat room ID regardless of who initiates
        val sortedIds = listOf(userId1, userId2).sorted()
        val chatRoomId = "chat_${queryId}_${sortedIds[0]}_${sortedIds[1]}"
        Log.d(TAG, "generateChatRoomId: queryId=$queryId, users=$sortedIds, result=$chatRoomId")
        return chatRoomId
    }
}