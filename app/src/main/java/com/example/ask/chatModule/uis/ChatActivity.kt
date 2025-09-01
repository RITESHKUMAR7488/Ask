package com.example.ask.chatModule.uis

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.chatModule.adapters.ChatAdapter
import com.example.ask.chatModule.models.ChatModel
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

    private var chatRoomId: String? = null
    private var queryModel: QueryModel? = null
    private val currentUserId by lazy { preferenceManager.userId }
    private val currentUser by lazy { preferenceManager.userModel }

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_QUERY = "extra_query"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get query data from intent
        getIntentData()

        // Setup UI components
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Initialize or get chat room
        initializeChatRoom()
    }

    private fun getIntentData() {
        queryModel = intent.getParcelableExtra(EXTRA_QUERY)
        Log.d(TAG, "getIntentData: Received query - ${queryModel?.title}")

        if (queryModel == null) {
            Log.e(TAG, "getIntentData: No query data received")
            motionToastUtil.showFailureToast(this, "Error: No query data received")
            finish()
            return
        }

        if (currentUserId.isNullOrEmpty() || currentUser == null) {
            Log.e(TAG, "getIntentData: Current user data is missing")
            motionToastUtil.showFailureToast(this, "Error: User not logged in")
            finish()
            return
        }
    }

    private fun setupUI() {
        queryModel?.let { query ->
            with(binding) {
                // Set chat title
                tvChatTitle.text = "Chat about: ${query.title}"

                // Set query info
                tvQueryTitle.text = query.title
                tvQueryDescription.text = query.description
                tvQueryOwner.text = "Query by: ${query.userName}"
            }
        }
    }

    private fun setupRecyclerView() {
        if (currentUserId.isNullOrEmpty()) {
            Log.e(TAG, "setupRecyclerView: Current user ID is null")
            return
        }

        chatAdapter = ChatAdapter(
            context = this,
            currentUserId = currentUserId!!
        )

        with(binding.recyclerViewMessages) {
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

            // Send on enter key (optional)
            etMessage.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun observeViewModel() {
        // Observe chat room creation/retrieval
        chatViewModel.chatRoom.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG, "observeViewModel: Loading chat room...")
                    showProgress(true)
                }

                is UiState.Success -> {
                    Log.d(TAG, "observeViewModel: Chat room ready - ${state.data.chatRoomId}")
                    chatRoomId = state.data.chatRoomId
                    showProgress(false)
                    showChatLayout(true)
                }

                is UiState.Failure -> {
                    Log.e(TAG, "observeViewModel: Failed to create/get chat room - ${state.error}")
                    showProgress(false)
                    motionToastUtil.showFailureToast(this, "Failed to initialize chat: ${state.error}")
                    finish()
                }
            }
        }

        // Observe real-time messages
        chatViewModel.realTimeMessages.observe(this) { messages ->
            Log.d(TAG, "observeViewModel: Received ${messages.size} real-time messages")

            if (messages.isEmpty()) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                chatAdapter.submitList(messages) {
                    // Scroll to bottom after messages are updated
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        // Observe message sending
        chatViewModel.sendMessage.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG, "observeViewModel: Sending message...")
                    binding.btnSend.isEnabled = false
                }

                is UiState.Success -> {
                    Log.d(TAG, "observeViewModel: Message sent successfully")
                    binding.btnSend.isEnabled = true
                    binding.etMessage.text?.clear()
                }

                is UiState.Failure -> {
                    Log.e(TAG, "observeViewModel: Failed to send message - ${state.error}")
                    binding.btnSend.isEnabled = true
                    motionToastUtil.showFailureToast(this, "Failed to send message: ${state.error}")
                }
            }
        }

        // Observe mark as read
        chatViewModel.markAsRead.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    Log.d(TAG, "observeViewModel: Messages marked as read")
                }
                is UiState.Failure -> {
                    Log.e(TAG, "observeViewModel: Failed to mark messages as read - ${state.error}")
                }
                is UiState.Loading -> {
                    // Loading state for mark as read
                }
            }
        }
    }

    private fun initializeChatRoom() {
        val query = queryModel
        val user = currentUser
        val userId = currentUserId

        if (query == null || user == null || userId.isNullOrEmpty()) {
            Log.e(TAG, "initializeChatRoom: Missing required data")
            motionToastUtil.showFailureToast(this, "Error: Missing user or query data")
            finish()
            return
        }

        Log.d(TAG, "initializeChatRoom: Creating/getting chat room for query ${query.queryId}")

        chatViewModel.createOrGetChatRoom(
            queryId = query.queryId ?: "",
            queryTitle = query.title ?: "Unknown Query",
            queryOwnerId = query.userId ?: "",
            queryOwnerName = query.userName ?: "Unknown User",
            currentUserId = userId,
            currentUserName = user.fullName ?: "Unknown User"
        )
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text?.toString()?.trim()

        if (messageText.isNullOrEmpty()) {
            motionToastUtil.showWarningToast(this, "Please enter a message")
            return
        }

        val roomId = chatRoomId
        val user = currentUser
        val userId = currentUserId

        if (roomId.isNullOrEmpty() || user == null || userId.isNullOrEmpty()) {
            Log.e(TAG, "sendMessage: Missing required data")
            motionToastUtil.showFailureToast(this, "Error: Chat not initialized properly")
            return
        }

        Log.d(TAG, "sendMessage: Sending message - $messageText")

        chatViewModel.sendMessage(
            chatRoomId = roomId,
            messageText = messageText,
            senderId = userId,
            senderName = user.fullName ?: "Unknown User",
            senderImageUrl = user.imageUrl
        )
    }

    private fun showProgress(show: Boolean) {
        with(binding) {
            if (show) {
                progressBar.visibility = View.VISIBLE
                layoutChat.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showChatLayout(show: Boolean) {
        binding.layoutChat.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        with(binding) {
            if (show) {
                layoutEmptyChat.visibility = View.VISIBLE
                recyclerViewMessages.visibility = View.GONE
            } else {
                layoutEmptyChat.visibility = View.GONE
                recyclerViewMessages.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark messages as read when user opens the chat
        chatRoomId?.let { roomId ->
            currentUserId?.let { userId ->
                chatViewModel.markMessagesAsRead(roomId, userId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up listeners")
        // ViewModel will handle cleanup in onCleared()
    }
}