package com.example.ask.chatModule.uis

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.chatModule.adapters.ChatListAdapter
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.ActivityChatListBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatListActivity : BaseActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_list)

        setupViews()
        setupRecyclerView()
        setupObservers()
        loadUserChats()
    }

    private fun setupViews() {
        binding.toolbarTitle.text = "My Chats"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserChats()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.secondary_color
        )
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""

        chatListAdapter = ChatListAdapter(
            context = this,
            currentUserId = currentUserId,
            onChatClick = { chat ->
                openChat(chat)
            }
        )

        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = chatListAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        chatViewModel.userChatsState.observe(this) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewChats.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        binding.recyclerViewChats.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "No chats yet.\nStart a conversation from a query!"
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewChats.visibility = View.VISIBLE
                        chatListAdapter.submitList(state.data)
                    }
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewChats.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Failed to load chats.\nPull down to refresh."

                    motionToastUtil.showFailureToast(this, "Failed to load chats: ${state.error}")
                }
            }
        }
    }

    private fun loadUserChats() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            chatViewModel.getUserChats(userId)
        } else {
            motionToastUtil.showFailureToast(this, "User not logged in")
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun openChat(chat: ChatModel) {
        val currentUserId = preferenceManager.userId ?: return

        // Find the other participant
        val otherUserId = chat.participants?.find { it != currentUserId }
        val otherUserName = chat.participantNames?.get(otherUserId)
        val otherUserImage = chat.participantImages?.get(otherUserId)

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CHAT_ID, chat.chatId)
            putExtra(ChatActivity.EXTRA_QUERY_ID, chat.queryId)
            putExtra(ChatActivity.EXTRA_QUERY_TITLE, chat.queryTitle)
            putExtra(ChatActivity.EXTRA_RECEIVER_ID, otherUserId)
            putExtra(ChatActivity.EXTRA_RECEIVER_NAME, otherUserName)
            putExtra(ChatActivity.EXTRA_RECEIVER_IMAGE, otherUserImage)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadUserChats()
    }
}