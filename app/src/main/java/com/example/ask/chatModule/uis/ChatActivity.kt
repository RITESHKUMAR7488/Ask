package com.example.ask.chatModule.uis

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.chatModule.adapters.MessageAdapter
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.ActivityChatBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private var currentChat: ChatModel? = null
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverImage: String? = null

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_QUERY_ID = "extra_query_id"
        const val EXTRA_QUERY_TITLE = "extra_query_title"
        const val EXTRA_RECEIVER_ID = "extra_receiver_id"
        const val EXTRA_RECEIVER_NAME = "extra_receiver_name"
        const val EXTRA_RECEIVER_IMAGE = "extra_receiver_image"
        const val EXTRA_CHAT_ID = "extra_chat_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        extractIntentData()
        setupViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Check if we have an existing chat ID or need to create/get one
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        if (chatId != null) {
            // Load existing chat
            loadChat(chatId)
        } else {
            // Create or get chat
            createOrGetChat()
        }
    }

    private fun extractIntentData() {
        receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID)
        receiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME)
        receiverImage = intent.getStringExtra(EXTRA_RECEIVER_IMAGE)

        Log.d(TAG, "Receiver: $receiverName (ID: $receiverId)")
    }

    private fun setupViews() {
        binding.toolbarTitle.text = receiverName ?: "Chat"

        // Setup text watcher for send button enable/disable
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.isEnabled = !s.isNullOrBlank()
            }
        })
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""
        messageAdapter = MessageAdapter(
            context = this,
            currentUserId = currentUserId,
            onMessageLongClick = { message ->
                // Handle long click for edit/delete
                if (message.senderId == currentUserId) {
                    showMessageOptions(message.messageId ?: "", message.message ?: "")
                }
            }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupObservers() {
        // Observe chat creation
        chatViewModel.createChatState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    currentChat = state.data
                    Log.d(TAG, "Chat created/retrieved: ${state.data.chatId}")

                    // Start loading messages
                    state.data.chatId?.let { chatId ->
                        loadMessages(chatId)
                        markMessagesAsRead(chatId)
                    }
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showFailureToast(this, "Failed to start chat: ${state.error}")
                    Log.e(TAG, "Failed to create/get chat: ${state.error}")
                }
            }
        }

        // Observe messages
        lifecycleScope.launch {
            chatViewModel.messagesFlow.collect { messages ->
                Log.d(TAG, "Received ${messages.size} messages")
                messageAdapter.submitList(messages)

                // Scroll to bottom when new message arrives
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }

                // Show/hide empty state
                if (messages.isEmpty()) {
                    binding.tvEmptyChat.visibility = View.VISIBLE
                    binding.recyclerViewMessages.visibility = View.GONE
                } else {
                    binding.tvEmptyChat.visibility = View.GONE
                    binding.recyclerViewMessages.visibility = View.VISIBLE
                }
            }
        }

        // Observe send message state
        chatViewModel.sendMessageState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnSend.isEnabled = false
                }
                is UiState.Success -> {
                    binding.btnSend.isEnabled = true
                    binding.etMessage.text?.clear()
                }
                is UiState.Failure -> {
                    binding.btnSend.isEnabled = true
                    motionToastUtil.showFailureToast(this, "Failed to send message")
                    Log.e(TAG, "Failed to send message: ${state.error}")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttachment.setOnClickListener {
            // TODO: Implement file/image attachment
            motionToastUtil.showInfoToast(this, "Attachment feature coming soon!")
        }
    }

    private fun createOrGetChat() {
        val queryId = intent.getStringExtra(EXTRA_QUERY_ID) ?: ""
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE) ?: "Query Chat"
        val currentUserId = preferenceManager.userId ?: return
        val currentUser = preferenceManager.userModel ?: return

        if (receiverId == null) {
            motionToastUtil.showFailureToast(this, "Invalid receiver")
            finish()
            return
        }

        val participantIds = listOf(currentUserId, receiverId!!)
        val participantNames = mapOf(
            currentUserId to (currentUser.fullName ?: "Unknown"),
            receiverId!! to (receiverName ?: "Unknown")
        )
        val participantImages = mapOf(
            currentUserId to (currentUser.imageUrl ?: ""),
            receiverId!! to (receiverImage ?: "")
        )

        chatViewModel.createOrGetChat(
            queryId = queryId,
            queryTitle = queryTitle,
            participantIds = participantIds,
            participantNames = participantNames,
            participantImages = participantImages
        )
    }

    private fun loadChat(chatId: String) {
        loadMessages(chatId)
        markMessagesAsRead(chatId)
    }

    private fun loadMessages(chatId: String) {
        Log.d(TAG, "Loading messages for chat: $chatId")
        chatViewModel.loadMessages(chatId)
    }

    private fun markMessagesAsRead(chatId: String) {
        val currentUserId = preferenceManager.userId ?: return
        chatViewModel.markMessagesAsRead(chatId, currentUserId)
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) return

        val chatId = currentChat?.chatId ?: return
        val currentUser = preferenceManager.userModel ?: return
        val currentUserId = preferenceManager.userId ?: return

        chatViewModel.sendMessage(
            chatId = chatId,
            senderId = currentUserId,
            senderName = currentUser.fullName ?: "Unknown",
            senderImage = currentUser.imageUrl,
            message = message
        )
    }

    private fun showMessageOptions(messageId: String, currentMessage: String) {
        val options = arrayOf("Edit", "Delete", "Cancel")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(messageId, currentMessage)
                    1 -> confirmDelete(messageId)
                }
            }
            .show()
    }

    private fun showEditDialog(messageId: String, currentMessage: String) {
        val editText = android.widget.EditText(this)
        editText.setText(currentMessage)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newMessage = editText.text.toString().trim()
                if (newMessage.isNotEmpty() && newMessage != currentMessage) {
                    currentChat?.chatId?.let { chatId ->
                        chatViewModel.editMessage(chatId, messageId, newMessage)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(messageId: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                currentChat?.chatId?.let { chatId ->
                    chatViewModel.deleteMessage(chatId, messageId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}