package com.example.ask.chatModule.repositories

import android.util.Log
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
        private const val CHAT_ROOMS = "chatRooms"
        private const val MESSAGES = "messages"
    }

    private var messageListener: ListenerRegistration? = null

    override fun createChatRoom(
        chatRoom: ChatRoomModel,
        result: (UiState<ChatRoomModel>) -> Unit
    ) {
        val chatRoomRef = firestore.collection(CHAT_ROOMS).document()
        chatRoom.chatRoomId = chatRoomRef.id
        chatRoom.createdAt = System.currentTimeMillis()

        chatRoomRef.set(chatRoom)
            .addOnSuccessListener {
                Log.d(TAG, "Chat room created successfully: ${chatRoom.chatRoomId}")
                result(UiState.Success(chatRoom))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to create chat room", exception)
                result(UiState.Failure(exception.message ?: "Failed to create chat room"))
            }
    }

    override fun getChatRooms(
        userId: String,
        result: (UiState<List<ChatRoomModel>>) -> Unit
    ) {
        firestore.collection(CHAT_ROOMS)
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val chatRooms = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(ChatRoomModel::class.java)
                }
                Log.d(TAG, "Retrieved ${chatRooms.size} chat rooms for user: $userId")
                result(UiState.Success(chatRooms))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get chat rooms", exception)
                result(UiState.Failure(exception.message ?: "Failed to get chat rooms"))
            }
    }

    override fun sendMessage(
        message: MessageModel,
        result: (UiState<String>) -> Unit
    ) {
        val messageRef = firestore.collection(CHAT_ROOMS)
            .document(message.chatRoomId!!)
            .collection(MESSAGES)
            .document()

        message.messageId = messageRef.id
        message.timestamp = System.currentTimeMillis()

        // Add message to subcollection
        messageRef.set(message)
            .addOnSuccessListener {
                // Update chat room's last message info
                updateChatRoomLastMessage(message)
                Log.d(TAG, "Message sent successfully: ${message.messageId}")
                result(UiState.Success("Message sent successfully"))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to send message", exception)
                result(UiState.Failure(exception.message ?: "Failed to send message"))
            }
    }

    private fun updateChatRoomLastMessage(message: MessageModel) {
        val chatRoomRef = firestore.collection(CHAT_ROOMS).document(message.chatRoomId!!)
        val updates = mapOf(
            "lastMessage" to message.message,
            "lastMessageTime" to message.timestamp,
            "lastMessageSenderId" to message.senderId
        )

        chatRoomRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Chat room last message updated")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to update chat room last message", exception)
            }
    }

    override fun getMessages(
        chatRoomId: String,
        result: (UiState<List<MessageModel>>) -> Unit
    ) {
        firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val messages = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(MessageModel::class.java)
                }
                Log.d(TAG, "Retrieved ${messages.size} messages for chat room: $chatRoomId")
                result(UiState.Success(messages))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get messages", exception)
                result(UiState.Failure(exception.message ?: "Failed to get messages"))
            }
    }

    override fun listenToMessages(
        chatRoomId: String,
        onMessageReceived: (List<MessageModel>) -> Unit
    ) {
        removeMessageListener() // Remove any existing listener

        messageListener = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e(TAG, "Listen to messages failed", exception)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val messages = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(MessageModel::class.java)
                    }
                    Log.d(TAG, "Real-time messages updated: ${messages.size} messages")
                    onMessageReceived(messages)
                }
            }
    }

    override fun markMessageAsRead(
        chatRoomId: String,
        messageId: String,
        userId: String
    ) {
        val messageRef = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(MESSAGES)
            .document(messageId)

        messageRef.get()
            .addOnSuccessListener { document ->
                val message = document.toObject(MessageModel::class.java)
                if (message != null && message.senderId != userId) {
                    val currentReadBy = message.readBy?.toMutableList() ?: mutableListOf()
                    if (!currentReadBy.contains(userId)) {
                        currentReadBy.add(userId)
                        messageRef.update("readBy", currentReadBy, "isRead", true)
                    }
                }
            }
    }

    override fun checkExistingChatRoom(
        currentUserId: String,
        targetUserId: String,
        result: (UiState<ChatRoomModel?>) -> Unit
    ) {
        firestore.collection(CHAT_ROOMS)
            .whereArrayContains("participants", currentUserId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingChatRoom = querySnapshot.documents.firstOrNull { document ->
                    val chatRoom = document.toObject(ChatRoomModel::class.java)
                    chatRoom?.participants?.contains(targetUserId) == true &&
                            chatRoom.participants?.size == 2 // Only 2 participants (private chat)
                }?.toObject(ChatRoomModel::class.java)

                Log.d(TAG, "Existing chat room check: ${existingChatRoom?.chatRoomId}")
                result(UiState.Success(existingChatRoom))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check existing chat room", exception)
                result(UiState.Failure(exception.message ?: "Failed to check existing chat room"))
            }
    }

    override fun removeMessageListener() {
        messageListener?.remove()
        messageListener = null
    }
}