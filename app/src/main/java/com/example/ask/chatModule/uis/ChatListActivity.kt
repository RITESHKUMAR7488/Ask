package com.example.ask.chatModule.uis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.chatModule.adapters.ChatRoomAdapter
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.viewModels.ChatListViewModel
import com.example.ask.databinding.ActivityChatListBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatListActivity : BaseActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val chatListViewModel: ChatListViewModel by viewModels()
    private lateinit var chatRoomAdapter: ChatRoomAdapter

    companion object {
        private const val TAG = "ChatListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_list)

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // ✅ One-time fix for existing chat rooms
        fixExistingChatRooms()

        loadChatRooms()
    }

    private fun fixExistingChatRooms() {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("chatRooms")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                var needsUpdate = false

                snapshot.documents.forEach { doc ->
                    val isActive = doc.getBoolean("isActive")
                    if (isActive == null) {
                        batch.update(doc.reference, "isActive", true)
                        needsUpdate = true
                        Log.d(TAG, "Fixing chat room: ${doc.id}")
                    }
                }

                if (needsUpdate) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Fixed existing chat rooms")
                            // Reload chat rooms after fix
                            loadChatRooms()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to fix chat rooms", e)
                        }
                } else {
                    Log.d(TAG, "No chat rooms needed fixing")
                }
            }
    }

    private fun setupUI() {
        binding.tvToolbarTitle.text = "Chats"
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""

        chatRoomAdapter = ChatRoomAdapter(
            context = this,
            currentUserId = currentUserId,
            onChatRoomClick = { chatRoom ->
                openChatRoom(chatRoom)
            }
        )

        binding.recyclerViewChatRooms.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = chatRoomAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadChatRooms()
        }
    }

    private fun loadChatRooms() {
        val userId = preferenceManager.userId
        Log.d(TAG, "loadChatRooms: userId = $userId")

        if (!userId.isNullOrEmpty()) {
            Log.d(TAG, "Loading chat rooms for user: $userId")
            chatListViewModel.getUserChatRooms(userId)
        } else {
            Log.e(TAG, "User ID is null or empty")
            motionToastUtil.showFailureToast(this, "User not logged in")
            finish()
        }
    }

    private fun observeViewModel() {
        chatListViewModel.chatRooms.observe(this) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewChatRooms.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        binding.recyclerViewChatRooms.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewChatRooms.visibility = View.VISIBLE
                        chatRoomAdapter.submitList(state.data)
                    }
                }

                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewChatRooms.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    motionToastUtil.showFailureToast(this, "Failed to load chats: ${state.error}")
                }
            }
        }
    }

    private fun openChatRoom(chatRoom: ChatRoomModel) {
        // Convert ChatRoomModel to QueryModel for ChatActivity
        val queryModel = com.example.ask.addModule.models.QueryModel(
            queryId = chatRoom.queryId,
            title = chatRoom.queryTitle,
            userId = chatRoom.queryOwnerId,
            userName = chatRoom.queryOwnerName
        )

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_QUERY, queryModel)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ChatListActivity onResume - reloading chat rooms")
        loadChatRooms()

        // Add debug method
        debugUserData()
    }

    private fun debugUserData() {
        val userId = preferenceManager.userId
        val userModel = preferenceManager.userModel

        Log.d(TAG, "=== DEBUG USER DATA ===")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "User Model: ${userModel?.fullName} (${userModel?.email})")
        Log.d(TAG, "Is Logged In: ${preferenceManager.isLoggedIn}")
        Log.d(TAG, "=====================")

        // Also check chat rooms in Firestore manually
        if (!userId.isNullOrEmpty()) {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("chatRooms")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d(TAG, "=== ALL CHAT ROOMS DEBUG ===")
                    snapshot.documents.forEach { doc ->
                        val participants = doc.get("participants")
                        val queryTitle = doc.getString("queryTitle")
                        val isActive = doc.getBoolean("isActive")
                        val lastMessage = doc.getString("lastMessage")

                        Log.d(TAG, "Chat ID: ${doc.id}")
                        Log.d(TAG, "  Participants: $participants")
                        Log.d(TAG, "  Query: $queryTitle")
                        Log.d(TAG, "  Active: $isActive")
                        Log.d(TAG, "  Last Message: $lastMessage")
                        Log.d(TAG, "  Contains User: ${(participants as? List<*>)?.contains(userId)}")
                        Log.d(TAG, "---")
                    }
                    Log.d(TAG, "========================")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to debug chat rooms", e)
                }
        }
    }
}