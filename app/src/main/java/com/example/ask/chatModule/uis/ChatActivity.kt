// File: app/src/main/java/com/example/ask/chatModule/uis/ChatActivity.kt
package com.example.ask.chatModule.uis

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.chatModule.adapters.ChatAdapter
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.ActivityChatBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private var currentChatRoom: ChatRoomModel? = null
    private var queryModel: QueryModel? = null

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_QUERY = "extra_query"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        // Get query from intent
        queryModel = intent.getParcelableExtra(EXTRA_QUERY)

        if (queryModel == null) {
            motionToastUtil.showFailureToast(this, "Invalid query data")
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        createOrJoinChatRoom()
        setupBackPressHandling()
    }

    private fun setupUI() {
        with(binding) {
            // Set toolbar title to query title
            tvChatTitle.text = queryModel?.title ?: "Chat"
            tvQueryTitle.text = queryModel?.title
            tvQueryDescription.text = queryModel?.description

            // Show query owner info
            tvQueryOwner.text = "Query by: ${queryModel?.userName}"
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""

        chatAdapter = ChatAdapter(
            context = this,
            currentUserId = currentUserId
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // Start from bottom
            }
            adapter = chatAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            // Back button
            btnBack.setOnClickListener {
                finish()
            }

            // Send button
            btnSend.setOnClickListener {
                sendMessage()
            }

            // Send message on enter key (if needed)
            etMessage.setOnEditorActionListener { _, _, _ ->
                sendMessage()
                true
            }
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Mark messages as read before leaving
                currentChatRoom?.let { room ->
                    val userId = preferenceManager.userId
                    if (!userId.isNullOrEmpty()) {
                        chatViewModel.markMessagesAsRead(room.chatRoomId!!, userId)
                    }
                }

                // Close activity
                finish()
            }
        })
    }

    private fun createOrJoinChatRoom() {
        val query = queryModel ?: return
        val currentUser = preferenceManager.userModel
        val currentUserId = preferenceManager.userId

        if (currentUser == null || currentUserId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Please login to chat")
            finish()
            return
        }

        if (query.userId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Invalid query owner")
            finish()
            return
        }

        Log.d(TAG, "Creating/joining chat room for query: ${query.queryId}")

        chatViewModel.createOrGetChatRoom(
            queryId = query.queryId ?: "",
            queryTitle = query.title ?: "",
            queryOwnerId = query.userId!!,
            queryOwnerName = query.userName ?: "Unknown User",
            currentUserId = currentUserId,
            currentUserName = currentUser.fullName ?: "Unknown User"
        )
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isBlank()) {
            motionToastUtil.showWarningToast(this, "Please enter a message")
            return
        }

        val chatRoom = currentChatRoom ?: run {
            motionToastUtil.showFailureToast(this, "Chat room not ready")
            return
        }

        val currentUser = preferenceManager.userModel ?: run {
            motionToastUtil.showFailureToast(this, "User not logged in")
            return
        }

        val currentUserId = preferenceManager.userId ?: run {
            motionToastUtil.showFailureToast(this, "User ID not found")
            return
        }

        Log.d(TAG, "Sending message: $messageText")

        chatViewModel.sendMessage(
            chatRoomId = chatRoom.chatRoomId!!,
            messageText = messageText,
            senderId = currentUserId,
            senderName = currentUser.fullName ?: "Unknown User",
            senderImageUrl = currentUser.imageUrl
        )

        // Clear input field
        binding.etMessage.text?.clear()
    }

    private fun observeViewModel() {
        // Observe chat room creation
        chatViewModel.chatRoom.observe(this) { state ->
            Log.d(TAG, "Chat room state: $state")

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutChat.visibility = View.GONE
                }

                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutChat.visibility = View.VISIBLE

                    currentChatRoom = state.data
                    Log.d(TAG, "Chat room ready: ${state.data.chatRoomId}")

                    // Load initial messages
                    chatViewModel.loadMessages(state.data.chatRoomId!!)
                }

                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutChat.visibility = View.VISIBLE

                    Log.e(TAG, "Failed to create/get chat room: ${state.error}")
                    motionToastUtil.showFailureToast(this, "Failed to join chat: ${state.error}")
                }
            }
        }

        // Observe message sending
        chatViewModel.sendMessage.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnSend.isEnabled = false
                }

                is UiState.Success -> {
                    binding.btnSend.isEnabled = true
                    Log.d(TAG, "Message sent successfully")

                    // Scroll to bottom after sending
                    scrollToBottom()
                }

                is UiState.Failure -> {
                    binding.btnSend.isEnabled = true
                    Log.e(TAG, "Failed to send message: ${state.error}")
                    motionToastUtil.showFailureToast(this, "Failed to send message")
                }
            }
        }

        // Observe initial message loading
        chatViewModel.messages.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG, "Loading messages...")
                }

                is UiState.Success -> {
                    Log.d(TAG, "Initial messages loaded: ${state.data.size}")
                    chatAdapter.submitList(state.data)
                    scrollToBottom()
                }

                is UiState.Failure -> {
                    Log.e(TAG, "Failed to load messages: ${state.error}")
                    motionToastUtil.showFailureToast(this, "Failed to load messages")
                }
            }
        }

        // Observe real-time message updates
        chatViewModel.realTimeMessages.observe(this) { messages ->
            Log.d(TAG, "Real-time messages update: ${messages.size}")
            chatAdapter.submitList(messages)

            // Auto-scroll to bottom when new messages arrive
            if (messages.isNotEmpty()) {
                scrollToBottom()
            }
        }

        // Observe mark as read
        chatViewModel.markAsRead.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    Log.d(TAG, "Messages marked as read")
                }

                is UiState.Failure -> {
                    Log.e(TAG, "Failed to mark messages as read: ${state.error}")
                }

                is UiState.Loading -> {
                    // Handle loading if needed
                }
            }
        }
    }

    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            binding.recyclerViewMessages.post {
                binding.recyclerViewMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark messages as read when activity resumes
        currentChatRoom?.let { room ->
            val userId = preferenceManager.userId
            if (!userId.isNullOrEmpty()) {
                chatViewModel.markMessagesAsRead(room.chatRoomId!!, userId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatActivity destroyed")
    }
}