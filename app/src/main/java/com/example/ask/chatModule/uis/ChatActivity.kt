package com.example.ask.chatModule.uis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.example.ask.R
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.ActivityChatBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()

    companion object {
        private const val TAG = "ChatActivity"
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USER_NAME = "extra_user_name"
        private const val EXTRA_QUERY_TITLE = "extra_query_title"

        fun newIntent(
            context: Context,
            userId: String,
            userName: String,
            queryTitle: String? = null
        ): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_QUERY_TITLE, queryTitle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        setupToolbar()
        setupCometChatTheme()
        handleIntent()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat"
    }

    private fun setupCometChatTheme() {
        // Basic theme setup - CometChat v4 uses different theming approach
        // You can customize colors and themes through your app's theme
        Log.d(TAG, "Setting up CometChat theme")
    }

    private fun handleIntent() {
        val targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        val userName = intent.getStringExtra(EXTRA_USER_NAME)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        Log.d(TAG, "Chat with user: $userName ($targetUserId), Query: $queryTitle")

        if (!targetUserId.isNullOrEmpty()) {
            // Ensure current user is logged into CometChat
            chatViewModel.ensureCometChatLogin { success ->
                if (success) {
                    startDirectChat(targetUserId, userName, queryTitle)
                } else {
                    motionToastUtil.showFailureToast(
                        this,
                        "Failed to initialize chat. Please try again."
                    )
                    finish()
                }
            }
        } else {
            // Show conversation list if no specific user
            showConversationList()
        }
    }

    private fun startDirectChat(userId: String, userName: String?, queryTitle: String?) {
        // Update toolbar title
        supportActionBar?.title = userName ?: "Chat"

        // For CometChat v4, we'll create a simple chat implementation
        // This is a basic implementation - you can enhance it with CometChat UI components

        Log.d(TAG, "Starting direct chat with user: $userId")

        // Create a simple chat fragment placeholder
        val chatFragment = SimpleChatFragment.newInstance(userId, userName, queryTitle)

        // Add the chat fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.chat_container, chatFragment)
            .commit()
    }

    private fun showConversationList() {
        supportActionBar?.title = "Conversations"

        Log.d(TAG, "Showing conversation list")

        // Create a simple conversation list fragment
        val conversationFragment = ConversationListFragment.newInstance()

        // Add the conversation list fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.chat_container, conversationFragment)
            .commit()
    }

    private fun observeViewModel() {
        chatViewModel.loginState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Show loading if needed
                    Log.d(TAG, "CometChat login in progress...")
                }
                is UiState.Success -> {
                    Log.d(TAG, "CometChat login successful")
                    motionToastUtil.showSuccessToast(this, "Chat initialized successfully")
                }
                is UiState.Failure -> {
                    Log.e(TAG, "CometChat login failed: ${state.error}")
                    motionToastUtil.showFailureToast(this, "Chat initialization failed: ${state.error}")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure CometChat connection is active
        chatViewModel.ensureCometChatLogin { /* handled in observer */ }
    }
}