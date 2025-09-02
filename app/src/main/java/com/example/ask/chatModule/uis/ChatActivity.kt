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
    private var isInitialLoad = true

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
            Log.e(TAG, "Query model is null")
            motionToastUtil.showFailureToast(this, "Invalid query data")
            finish()
            return
        }

        Log.d(TAG, "Chat activity started for query: ${queryModel?.title}")

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        createOrJoinChatRoom()
        setupBackPressHandling()
    }

    private fun setupUI() {
        with(binding) {
            tvChatTitle.text = queryModel?.title ?: "Chat"
            tvQueryTitle.text = queryModel?.title
            tvQueryDescription.text = queryModel?.description
            tvQueryOwner.text = "Query by: ${queryModel?.userName}"
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""
        Log.d(TAG, "Setting up RecyclerView for user: $currentUserId")

        chatAdapter = ChatAdapter(
            context = this,
            currentUserId = currentUserId
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            btnBack.setOnClickListener {
                finish()
            }

            btnSend.setOnClickListener {
                sendMessage()
            }

            etMessage.setOnEditorActionListener { _, _, _ ->
                sendMessage()
                true
            }
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                markMessagesAsReadAndFinish()
            }
        })
    }

    private fun createOrJoinChatRoom() {
        val query = queryModel ?: return
        val currentUser = preferenceManager.userModel
        val currentUserId = preferenceManager.userId

        Log.d(TAG, "Creating chat room - CurrentUser: $currentUserId, QueryOwner: ${query.userId}")

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

        val chatRoom = currentChatRoom
        if (chatRoom?.chatRoomId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Chat room not ready")
            return
        }

        val currentUser = preferenceManager.userModel
        val currentUserId = preferenceManager.userId

        if (currentUser == null || currentUserId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "User not logged in")
            return
        }

        Log.d(TAG, "Sending message: $messageText to room: ${chatRoom.chatRoomId}")

        chatViewModel.sendMessage(
            chatRoomId = chatRoom.chatRoomId!!,
            messageText = messageText,
            senderId = currentUserId,
            senderName = currentUser.fullName ?: "Unknown User",
            senderImageUrl = currentUser.imageUrl
        )

        // Clear input field immediately for better UX
        binding.etMessage.text?.clear()
    }

    private fun observeViewModel() {
        // Observe chat room creation
        chatViewModel.chatRoom.observe(this) { state ->
            Log.d(TAG, "Chat room state changed: $state")

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

                    // Don't load initial messages here - let real-time listener handle it
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
                }

                is UiState.Failure -> {
                    binding.btnSend.isEnabled = true
                    Log.e(TAG, "Failed to send message: ${state.error}")
                    motionToastUtil.showFailureToast(this, "Failed to send message")
                }
            }
        }

        // Observe real-time message updates (PRIMARY SOURCE)
        chatViewModel.realTimeMessages.observe(this) { messages ->
            Log.d(TAG, "Real-time messages update: ${messages.size} messages")

            if (messages.isNotEmpty()) {
                chatAdapter.submitList(messages) {
                    // Scroll to bottom after list is updated
                    scrollToBottom()
                }

                // Mark messages as read when new messages arrive
                if (!isInitialLoad) {
                    markMessagesAsRead()
                }
                isInitialLoad = false
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
                try {
                    binding.recyclerViewMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scrolling to bottom", e)
                }
            }
        }
    }

    private fun markMessagesAsRead() {
        currentChatRoom?.let { room ->
            val userId = preferenceManager.userId
            if (!userId.isNullOrEmpty()) {
                chatViewModel.markMessagesAsRead(room.chatRoomId!!, userId)
            }
        }
    }

    private fun markMessagesAsReadAndFinish() {
        markMessagesAsRead()
        // Stop real-time listener
        chatViewModel.stopRealtimeMessageListener()
        finish()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ChatActivity resumed")
        markMessagesAsRead()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ChatActivity paused")
        markMessagesAsRead()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatActivity destroyed")
        chatViewModel.stopRealtimeMessageListener()
    }
}