package com.example.ask.chatModule.uis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.ui.feature.messages.MessageListActivity

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    companion object {
        private const val EXTRA_CHANNEL_ID = "extra_channel_id"
        private const val EXTRA_QUERY_TITLE = "extra_query_title"

        fun createIntent(context: Context, channelId: String, queryTitle: String): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ID, channelId)
                putExtra(EXTRA_QUERY_TITLE, queryTitle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
        val queryTitle = intent.getStringExtra(EXTRA_QUERY_TITLE)

        if (channelId != null) {
            // Start Stream's MessageListActivity with proper CID format
            val messageIntent = MessageListActivity.createIntent(
                context = this,
                cid = "messaging:$channelId"
            )
            startActivity(messageIntent)
            finish()
        } else {
            motionToastUtil.showFailureToast(this, "Invalid channel ID")
            finish()
        }
    }
}