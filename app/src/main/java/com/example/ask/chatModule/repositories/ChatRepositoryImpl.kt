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
        Log.d(TAG, "createOrGetChatRoom: queryId=$queryId")

        val chatRoomId = generateChatRoomId(queryId, queryOwnerId, currentUserId)
        val chatRoomRef = firestore.collection(CHAT_ROOMS).document(chatRoomId)

        // Check if chat room already exists
        chatRoomRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Chat room exists, return it
                    Log.d(TAG, "Chat room exists, returning existing room")
                    val chatRoom = document.toObject(ChatRoomModel::class.java)
                    if (chatRoom != null) {
                        result.invoke(UiState.Success(chatRoom))
                    } else {
                        result.invoke(UiState.Failure("Failed to parse existing chat room"))
                    }
                } else {
                    // Create new chat room
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
            isActive = true // ✅ Make sure this is set
        )

        Log.d(TAG, "Chat room data: $chatRoom")

        // ✅ Create a map to ensure isActive field is properly saved
        val chatRoomData = mapOf(
            "chatRoomId" to chatRoom.chatRoomId,
            "queryId" to chatRoom.queryId,
            "queryTitle" to chatRoom.queryTitle,
            "queryOwnerId" to chatRoom.queryOwnerId,
            "queryOwnerName" to chatRoom.queryOwnerName,
            "participants" to chatRoom.participants,
            "participantDetails" to chatRoom.participantDetails,
            "lastMessage" to chatRoom.lastMessage,
            "lastMessageTime" to chatRoom.lastMessageTime,
            "lastMessageSenderId" to chatRoom.lastMessageSenderId,
            "createdAt" to chatRoom.createdAt,
            "isActive" to true // ✅ Explicitly set as true
        )

        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .set(chatRoomData) // Use the map instead of the object
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

        val messageId = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .document().id

        message.messageId = messageId

        // Add message to subcollection
        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .document(messageId)
            .set(message)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")

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
                    doc.toObject(ChatModel::class.java)
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
                        doc.toObject(ChatModel::class.java)
                    }
                    Log.d(TAG, "Real-time update: ${messages.size} messages")
                    callback(messages)
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
        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = firestore.batch()

                querySnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }

                batch.commit()
                    .addOnSuccessListener {
                        result.invoke(UiState.Success("Messages marked as read"))
                    }
                    .addOnFailureListener { exception ->
                        result.invoke(UiState.Failure(exception.message ?: "Failed to mark messages as read"))
                    }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.message ?: "Failed to update read status"))
            }
    }

    private fun generateChatRoomId(queryId: String, userId1: String, userId2: String): String {
        val sortedIds = listOf(userId1, userId2).sorted()
        return "chat_${queryId}_${sortedIds[0]}_${sortedIds[1]}"
    }
    private fun sendChatNotification(
        chatRoom: ChatRoomModel,
        currentUserId: String,
        currentUserName: String
    ) {
        // Find the other participant (receiver)
        val receiverId = chatRoom.participants?.firstOrNull { it != currentUserId }

        if (receiverId != null) {
            val notification = mapOf(
                "type" to "NEW_CHAT",
                "title" to "New Chat Started",
                "message" to "$currentUserName started a chat about: ${chatRoom.queryTitle}",
                "senderId" to currentUserId,
                "senderName" to currentUserName,
                "targetUserId" to receiverId,
                "chatRoomId" to chatRoom.chatRoomId,
                "queryId" to chatRoom.queryId,
                "queryTitle" to chatRoom.queryTitle,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )

            firestore.collection("notifications")
                .document()
                .set(notification)
                .addOnSuccessListener {
                    Log.d(TAG, "Chat notification sent successfully")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to send chat notification", exception)
                }
        }
    }
    fun fixExistingChatRooms() {
        firestore.collection(CHAT_ROOMS)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()

                snapshot.documents.forEach { doc ->
                    val isActive = doc.getBoolean("isActive")
                    if (isActive == null) {
                        // Set isActive to true for all existing chat rooms
                        batch.update(doc.reference, "isActive", true)
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Fixed existing chat rooms isActive field")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to fix existing chat rooms", e)
                    }
            }
    }
}