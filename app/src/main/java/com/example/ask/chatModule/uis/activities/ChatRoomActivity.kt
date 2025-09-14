package com.example.ask.chatModule.uis.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.chatModule.adapters.MessageAdapter
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.chatModule.models.TypingIndicator
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.ActivityChatRoomBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState

// IMPORTANT: No @AndroidEntryPoint here because BaseActivity already has it.
class ChatRoomActivity : BaseActivity() {

    private lateinit var binding: ActivityChatRoomBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private var chatRoom: ChatRoomModel? = null

    // Store only primitives/strings, not the whole user object to avoid type/package issues.
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var currentUserImage: String? = null

    companion object {
        private const val TAG = "ChatRoomActivity"
        const val EXTRA_CHAT_ROOM = "CHAT_ROOM"
        const val EXTRA_TARGET_USER_ID = "TARGET_USER_ID"
        const val EXTRA_TARGET_USER_NAME = "TARGET_USER_NAME"
        const val EXTRA_TARGET_USER_IMAGE = "TARGET_USER_IMAGE"
        const val EXTRA_QUERY_ID = "QUERY_ID"
        const val EXTRA_QUERY_TITLE = "QUERY_TITLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_room)

        // ✅ Safe to access injected fields now (after Activity is constructed).
        val userId = preferenceManager.userId
        val user = preferenceManager.userModel  // keep local; do not store as a mutable property

        currentUserId = userId
        currentUserName = user?.fullName ?: "User"
        currentUserImage = user?.imageUrl

        if (currentUserId.isNullOrEmpty() || user == null) {
            motionToastUtil.showFailureToast(this, "Please login to use chat feature")
            finish()
            return
        }

        // Get chat room data from intent
        getChatRoomFromIntent()

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        observeViewModel()

        if (chatRoom != null) {
            setupChatRoom()
        } else {
            createChatRoomFromIntent()
        }
    }

    private fun getChatRoomFromIntent() {
        chatRoom = intent.getParcelableExtra(EXTRA_CHAT_ROOM)

        chatRoom?.let {
            Log.d(TAG, "Existing chat room found: ${it.chatRoomId}")
            setupToolbarWithChatRoom(it)
        }
    }

    private fun setupChatRoom() {
        val roomId = chatRoom?.chatRoomId ?: return
        val userId = currentUserId ?: return
        val userName = currentUserName ?: return

        // Set current chat room for typing indicators
        chatViewModel.setCurrentChatRoom(roomId, userId, userName)

        loadMessages()
        listenToTypingIndicators()
    }

    private fun createChatRoomFromIntent() {
        val targetUserId = intent.getStringExtra(EXTRA_TARGET_USER_ID)
        val targetUserName = intent.getStringExtra(EXTRA_TARGET_USER_NAME)
        val targetUserImage = intent.getStringExtra(EXTRA_TARGET_USER_IMAGE)
        val queryId = intent.getStringExtra(EXTRA_QUERY_ID)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        if (targetUserId.isNullOrEmpty() || targetUserName.isNullOrEmpty() || currentUserId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Invalid chat parameters")
            finish()
            return
        }

        Log.d(TAG, "Creating new chat room with user: $targetUserName")

        chatViewModel.createOrGetChatRoom(
            currentUserId = currentUserId!!,
            currentUserName = currentUserName ?: "User",
            currentUserImage = currentUserImage,
            targetUserId = targetUserId,
            targetUserName = targetUserName,
            targetUserImage = targetUserImage,
            queryId = queryId,
            queryTitle = queryTitle
        )
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupToolbarWithChatRoom(chatRoom: ChatRoomModel) {
        val otherParticipantId = chatRoom.participants?.firstOrNull { it != currentUserId }
        val otherParticipantName = otherParticipantId?.let { chatRoom.participantNames?.get(it) } ?: "Chat"
        binding.toolbar.title = otherParticipantName
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            context = this,
            currentUserId = currentUserId!!
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatRoomActivity).apply {
                stackFromEnd = true // Start from bottom
            }
            adapter = messageAdapter
        }
    }

    private fun setupMessageInput() {
        binding.fabSend.setOnClickListener { sendMessage() }

        // Handle enter key in EditText
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Add text watcher for typing indicator
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // User is typing
                if (!s.isNullOrEmpty()) {
                    chatViewModel.onUserStartTyping()
                } else {
                    chatViewModel.onUserStopTyping()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty()) {
            motionToastUtil.showWarningToast(this, "Please enter a message")
            return
        }

        val roomId = chatRoom?.chatRoomId
        if (roomId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Chat room not ready yet")
            return
        }

        chatViewModel.sendMessage(
            chatRoomId = roomId,
            senderId = currentUserId!!,
            senderName = currentUserName ?: "User",
            senderImage = currentUserImage,
            messageText = messageText
        )

        // Clear input and stop typing indicator
        binding.etMessage.setText("")
        chatViewModel.onUserStopTyping()
    }

    private fun loadMessages() {
        val roomId = chatRoom?.chatRoomId ?: return
        Log.d(TAG, "Loading messages for chat room: $roomId")
        chatViewModel.listenToMessages(roomId)
    }

    private fun listenToTypingIndicators() {
        val roomId = chatRoom?.chatRoomId ?: return
        val userId = currentUserId ?: return

        Log.d(TAG, "Starting typing indicator listener for chat room: $roomId")
        chatViewModel.listenToTypingIndicators(roomId, userId)
    }

    private fun observeViewModel() {
        // Observe chat room creation
        chatViewModel.createChatRoom.observe(this) { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    chatRoom = state.data
                    setupToolbarWithChatRoom(chatRoom!!)
                    setupChatRoom()
                    Log.d(TAG, "Chat room created/retrieved: ${chatRoom?.chatRoomId}")
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showFailureToast(this, "Failed to create chat: ${state.error}")
                    Log.e(TAG, "Failed to create chat room: ${state.error}")
                }
            }
        }

        // Observe real-time messages
        chatViewModel.realTimeMessages.observe(this) { messages ->
            Log.d(TAG, "Received ${messages.size} messages")
            messageAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
            // Mark messages as read
            markMessagesAsRead(messages)
        }

        // Observe typing indicators
        chatViewModel.typingIndicators.observe(this) { typingUsers ->
            updateTypingIndicator(typingUsers)
        }

        // Observe send message result
        chatViewModel.sendMessage.observe(this) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Success -> Log.d(TAG, "Message sent successfully")
                is UiState.Failure -> {
                    motionToastUtil.showFailureToast(this, "Failed to send message: ${state.error}")
                    Log.e(TAG, "Failed to send message: ${state.error}")
                }
            }
        }
    }

    private fun updateTypingIndicator(typingUsers: List<TypingIndicator>) {
        if (typingUsers.isEmpty()) {
            // Hide typing indicator
            binding.tvTypingIndicator.visibility = View.GONE
        } else {
            // Show typing indicator
            val typingText = when (typingUsers.size) {
                1 -> "${typingUsers[0].userName} is typing..."
                2 -> "${typingUsers[0].userName} and ${typingUsers[1].userName} are typing..."
                else -> "Multiple users are typing..."
            }

            binding.tvTypingIndicator.text = typingText
            binding.tvTypingIndicator.visibility = View.VISIBLE

            Log.d(TAG, "Typing indicator: $typingText")
        }
    }

    private fun markMessagesAsRead(messages: List<MessageModel>) {
        val roomId = chatRoom?.chatRoomId ?: return
        val uid = currentUserId ?: return

        messages
            .asSequence()
            .filter { it.senderId != uid && (it.readBy?.contains(uid) != true) }
            .forEach { msg ->
                val messageId = msg.messageId ?: return@forEach
                chatViewModel.markMessageAsRead(
                    chatRoomId = roomId,
                    messageId = messageId,
                    userId = uid
                )
            }
    }

    override fun onPause() {
        super.onPause()
        // Stop typing indicator when user leaves the activity
        chatViewModel.onUserStopTyping()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.removeMessageListener()
        chatViewModel.removeTypingListener()
        chatViewModel.onUserStopTyping() // Ensure typing indicator is cleared
    }
}