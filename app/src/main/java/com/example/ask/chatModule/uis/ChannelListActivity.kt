package com.example.ask.chatModule.uis

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.ask.R
import com.example.ask.databinding.ActivityChannelListBinding
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.ui.feature.channels.ChannelListActivity

@AndroidEntryPoint
class ChannelListActivity : BaseActivity() {

    private lateinit var binding: ActivityChannelListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel_list)

        // Start Stream's ChannelListActivity
        startActivity(ChannelListActivity.createIntent(this))
        finish()
    }
}