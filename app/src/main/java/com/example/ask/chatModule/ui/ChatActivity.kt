package com.example.ask.chatModule.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.cometchat.chat.models.User
import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

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
            Log.d(TAG, "Starting chat with user: $targetUserId - $targetUserName")

            // Since CometChat UI components are having issues, use system messaging as primary approach
            val message = buildString {
                append("Hi ${targetUserName ?: "there"}!")
                append("\n\nI'd like to discuss your query")
                if (!queryTitle.isNullOrEmpty()) {
                    append(": \"$queryTitle\"")
                }
                append("\n\nPlease reply to continue our conversation about this topic.")
                append("\n\n---")
                append("\nSent from Ask App")
            }

            // Create sharing intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Regarding: ${queryTitle ?: "Your Query"}")
            }

            // Check if messaging apps are available
            if (shareIntent.resolveActivity(context.packageManager) != null) {
                Log.d(TAG, "Opening messaging apps chooser")
                val chooserIntent = Intent.createChooser(
                    shareIntent,
                    "Send message to ${targetUserName ?: "user"}"
                )
                context.startActivity(chooserIntent)
            } else {
                Log.e(TAG, "No messaging apps available")
                // Show error if context is BaseActivity
                if (context is BaseActivity) {
                    context.motionToastUtil.showFailureToast(
                        context,
                        "No messaging app available. Please install WhatsApp, SMS, or email app."
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "ChatActivity onCreate - Redirecting to built-in activity")

        // Get intent data and redirect
        val targetUserId = intent.getStringExtra(EXTRA_USER_ID)
        val targetUserName = intent.getStringExtra(EXTRA_USER_NAME)
        val targetUserAvatar = intent.getStringExtra(EXTRA_USER_AVATAR)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        if (!targetUserId.isNullOrEmpty()) {
            // Call the static method to handle the actual chat launching
            startChatActivity(this, targetUserId, targetUserName, targetUserAvatar, queryTitle)
        } else {
            motionToastUtil.showFailureToast(this, "No user selected for chat")
        }

        // Close this activity since we're using CometChat's activity or external messaging
        finish()
    }
}