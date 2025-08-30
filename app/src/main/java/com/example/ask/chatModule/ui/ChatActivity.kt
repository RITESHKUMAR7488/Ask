package com.example.ask.chatModule.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader
import com.cometchat.chatuikit.messagelist.CometChatMessageList
import com.example.ask.MyApplication
import com.example.ask.R
import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.databinding.ActivityChatContainerBinding
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatContainerBinding
    private var messageHeader: CometChatMessageHeader? = null
    private var messageList: CometChatMessageList? = null
    private var messageComposer: CometChatMessageComposer? = null

    @Inject
    lateinit var cometChatManager: CometChatManager

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_USER_AVATAR = "user_avatar"
        const val EXTRA_QUERY_TITLE = "query_title"

        fun startChatActivity(
            context: Context,
            targetUserId: String,
            targetUserName: String? = null,
            targetUserAvatar: String? = null,
            queryTitle: String? = null
        ) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, targetUserId)
                putExtra(EXTRA_USER_NAME, targetUserName)
                putExtra(EXTRA_USER_AVATAR, targetUserAvatar)
                putExtra(EXTRA_QUERY_TITLE, queryTitle)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "ChatActivity onCreate - Starting with safe approach")

        // Use container layout first (no CometChat components)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_container)

        // Get intent data
        val targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        val targetUserName = intent.getStringExtra(EXTRA_USER_NAME)
        val targetUserAvatar = intent.getStringExtra(EXTRA_USER_AVATAR)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        if (targetUserId.isNullOrEmpty()) {
            Log.e(TAG, "No target user specified for chat")
            motionToastUtil.showFailureToast(this, "No user selected for chat")
            finish()
            return
        }

        setupToolbar(targetUserName, queryTitle)
        setupBackPressHandler()

        // Wait for CometChat to be ready before creating UI components
        waitForCometChatAndSetup(targetUserId, targetUserName, targetUserAvatar, queryTitle)
    }

    private fun waitForCometChatAndSetup(
        targetUserId: String,
        targetUserName: String?,
        targetUserAvatar: String?,
        queryTitle: String?
    ) {
        Log.d(TAG, "Checking CometChat readiness...")

        if (MyApplication.isCometChatInitialized && cometChatManager.isCometChatLoggedIn()) {
            Log.d(TAG, "CometChat is ready, creating UI components")
            createCometChatUI(targetUserId, targetUserName, targetUserAvatar, queryTitle)
        } else {
            Log.d(TAG, "CometChat not ready, showing loading and waiting...")

            // Show loading state
            binding.progressBar.visibility = View.VISIBLE
            binding.tvLoading.visibility = View.VISIBLE

            // Wait for CometChat initialization
            cometChatManager.waitForInitialization { isInitialized ->
                runOnUiThread {
                    if (isInitialized) {
                        Log.d(TAG, "CometChat initialized, checking login status...")

                        if (cometChatManager.isCometChatLoggedIn()) {
                            Log.d(TAG, "User already logged in, creating UI")
                            createCometChatUI(targetUserId, targetUserName, targetUserAvatar, queryTitle)
                        } else {
                            Log.d(TAG, "User not logged in, attempting login...")
                            binding.tvLoading.text = "Connecting to chat..."

                            val userModel = preferenceManager.userModel
                            if (userModel?.uid != null) {
                                cometChatManager.loginToCometChat(userModel.uid!!) { success, _, message ->
                                    runOnUiThread {
                                        if (success) {
                                            Log.d(TAG, "Login successful, creating UI")
                                            createCometChatUI(targetUserId, targetUserName, targetUserAvatar, queryTitle)
                                        } else {
                                            Log.e(TAG, "Login failed: $message")
                                            showError("Failed to connect to chat: $message")
                                        }
                                    }
                                }
                            } else {
                                showError("User information not available")
                            }
                        }
                    } else {
                        Log.e(TAG, "CometChat initialization failed")
                        showError("Chat service unavailable")
                    }
                }
            }
        }
    }

    private fun createCometChatUI(
        targetUserId: String,
        targetUserName: String?,
        targetUserAvatar: String?,
        queryTitle: String?
    ) {
        try {
            Log.d(TAG, "Creating CometChat UI components programmatically")

            // Hide loading
            binding.progressBar.visibility = View.GONE
            binding.tvLoading.visibility = View.GONE

            // Create User object
            val targetUser = User().apply {
                uid = targetUserId
                name = targetUserName ?: "Unknown User"
                avatar = targetUserAvatar ?: ""
            }

            // Create CometChat UI components programmatically
            messageHeader = CometChatMessageHeader(this).apply {
                setUser(targetUser)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                elevation = 4f
            }

            messageList = CometChatMessageList(this).apply {
                setUser(targetUser)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f  // weight = 1
                )
            }

            messageComposer = CometChatMessageComposer(this).apply {
                setUser(targetUser)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                elevation = 4f
            }

            // Add components to container
            binding.chatContainer.removeAllViews() // Clear loading views

            messageHeader?.let { binding.chatContainer.addView(it) }
            messageList?.let { binding.chatContainer.addView(it) }
            messageComposer?.let { binding.chatContainer.addView(it) }

            Log.d(TAG, "✅ CometChat UI components created successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating CometChat UI components", e)
            showError("Failed to setup chat interface")
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.tvLoading.text = "Error: $message\n\nTap back to return"
            binding.tvLoading.visibility = View.VISIBLE

            motionToastUtil.showFailureToast(this, message)

            // Auto-close after 3 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish()
            }, 3000)
        }
    }

    private fun setupToolbar(targetUserName: String?, queryTitle: String?) {
        // Set activity title
        val title = when {
            !queryTitle.isNullOrEmpty() -> "Chat: $queryTitle"
            !targetUserName.isNullOrEmpty() -> "Chat with $targetUserName"
            else -> "Chat"
        }

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        supportActionBar?.setHomeActionContentDescription("Back")
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackPress()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up CometChat components
        messageHeader = null
        messageList = null
        messageComposer = null

        Log.d(TAG, "ChatActivity destroyed and cleaned up")
    }
}