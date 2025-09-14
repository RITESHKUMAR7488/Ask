package com.example.ask.chatModule.repositories

import android.util.Log
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.chatModule.models.TypingIndicator
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
        private const val TYPING_INDICATORS = "typingIndicators"
        private const val TYPING_TIMEOUT = 3000L // 3 seconds timeout for typing
    }

    private var messageListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null

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
                // Clear typing indicator when message is sent
                setUserTyping(message.chatRoomId!!, message.senderId!!, message.senderName ?: "", false)
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

    // New typing indicator methods
    override fun setUserTyping(
        chatRoomId: String,
        userId: String,
        userName: String,
        isTyping: Boolean
    ) {
        val typingRef = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(TYPING_INDICATORS)
            .document(userId)

        if (isTyping) {
            val typingIndicator = TypingIndicator(
                userId = userId,
                userName = userName,
                chatRoomId = chatRoomId,
                isTyping = true,
                timestamp = System.currentTimeMillis()
            )

            typingRef.set(typingIndicator)
                .addOnSuccessListener {
                    Log.d(TAG, "Typing indicator set for user: $userName")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to set typing indicator", exception)
                }
        } else {
            // Remove typing indicator when user stops typing
            typingRef.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Typing indicator removed for user: $userName")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to remove typing indicator", exception)
                }
        }
    }

    override fun listenToTypingIndicator(
        chatRoomId: String,
        currentUserId: String,
        onTypingChanged: (List<TypingIndicator>) -> Unit
    ) {
        removeTypingListener() // Remove any existing listener

        typingListener = firestore.collection(CHAT_ROOMS)
            .document(chatRoomId)
            .collection(TYPING_INDICATORS)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e(TAG, "Listen to typing indicators failed", exception)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val currentTime = System.currentTimeMillis()
                    val typingIndicators = querySnapshot.documents.mapNotNull { document ->
                        val typingIndicator = document.toObject(TypingIndicator::class.java)
                        // Filter out expired typing indicators and current user
                        if (typingIndicator != null &&
                            typingIndicator.userId != currentUserId &&
                            typingIndicator.timestamp != null &&
                            (currentTime - typingIndicator.timestamp!!) < TYPING_TIMEOUT) {
                            typingIndicator
                        } else {
                            // Remove expired typing indicator
                            if (typingIndicator?.timestamp != null &&
                                (currentTime - typingIndicator.timestamp!!) >= TYPING_TIMEOUT) {
                                document.reference.delete()
                            }
                            null
                        }
                    }

                    Log.d(TAG, "Typing indicators updated: ${typingIndicators.size} users typing")
                    onTypingChanged(typingIndicators)
                }
            }
    }

    override fun removeTypingListener() {
        typingListener?.remove()
        typingListener = null
    }
}