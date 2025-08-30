package com.example.ask.chatModule.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.messagelist.CometChatMessageList
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader
import com.example.ask.R
import com.example.ask.databinding.ActivityChatBinding
import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding

    @Inject
    lateinit var cometChatManager: CometChatManager

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_USER_AVATAR = "user_avatar"
        const val EXTRA_QUERY_TITLE = "query_title"

        /**
         * Helper method to start ChatActivity
         */
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        // Get user information from intent
        val targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        val targetUserName = intent.getStringExtra(EXTRA_USER_NAME)
        val targetUserAvatar = intent.getStringExtra(EXTRA_USER_AVATAR)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        if (targetUserId.isNullOrEmpty()) {
            Log.e(TAG, "Target user ID is null or empty")
            motionToastUtil.showFailureToast(this, "Invalid user information")
            finish()
            return
        }

        setupChatComponents(targetUserId, targetUserName, targetUserAvatar, queryTitle)
        setupToolbar(targetUserName, queryTitle)
        setupBackPressHandler()
    }

    private fun setupChatComponents(
        targetUserId: String,
        targetUserName: String?,
        targetUserAvatar: String?,
        queryTitle: String?
    ) {
        // Create User object for CometChat components
        val targetUser = User().apply {
            uid = targetUserId
            name = targetUserName ?: "Unknown User"
            avatar = targetUserAvatar ?: ""
        }

        // Setup Message Header
        binding.messageHeader.apply {
            setUser(targetUser)

            // Set back button click listener - either use the default behavior
            // or create a custom back button view if needed
            // For default behavior, you can remove this line as CometChat usually handles it automatically
        }

        // If you need a custom back button, you would need to create a View and set it:
        /*
        val backButton = ImageView(this@ChatActivity).apply {
            setImageResource(R.drawable.ic_arrow_back) // Use your own drawable
            setOnClickListener { handleBackPress() }
        }
        binding.messageHeader.setBackIcon(backButton)
        */

        // Setup Message List
        binding.messageList.apply {
            setUser(targetUser)
        }

        // Setup Message Composer
        binding.messageComposer.apply {
            setUser(targetUser)
        }

        Log.d(TAG, "Chat components setup completed for user: $targetUserId")
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
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back) // Set your back icon here
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
}