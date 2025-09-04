// ChatFragment.kt
package com.example.ask.chatModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.chatModule.adapters.ChatRoomAdapter
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.chatModule.viewModels.ChatViewModel
import com.example.ask.databinding.FragmentChatBinding
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatRoomAdapter: ChatRoomAdapter

    companion object {
        private const val TAG = "ChatFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        loadChatRooms()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Chats"
    }

    private fun setupRecyclerView() {
        val currentUserId = preferenceManager.userId ?: ""

        chatRoomAdapter = ChatRoomAdapter(
            context = requireContext(),
            currentUserId = currentUserId,
            onChatRoomClick = { chatRoom ->
                openChatRoom(chatRoom)
            }
        )

        binding.recyclerViewChatRooms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatRoomAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadChatRooms()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            com.example.ask.R.color.primary_color,
            com.example.ask.R.color.secondary_color
        )
    }

    private fun loadChatRooms() {
        val userId = preferenceManager.userId
        Log.d(TAG, "loadChatRooms: userId = $userId")

        if (!userId.isNullOrEmpty()) {
            chatViewModel.getChatRooms(userId)
        } else {
            Log.e(TAG, "loadChatRooms: userId is null or empty")
            motionToastUtil.showFailureToast(
                requireActivity(),
                "User not logged in. Please login to view chats."
            )
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        chatViewModel.chatRooms.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "observeViewModel: Received state: $state")
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG, "observeViewModel: Loading state")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewChatRooms.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                is UiState.Success -> {
                    Log.d(TAG, "observeViewModel: Success state with ${state.data.size} chat rooms")
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        Log.d(TAG, "observeViewModel: No chat rooms found, showing empty state")
                        binding.recyclerViewChatRooms.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        Log.d(TAG, "observeViewModel: Showing ${state.data.size} chat rooms")
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewChatRooms.visibility = View.VISIBLE

                        // Log each chat room for debugging
                        state.data.forEachIndexed { index, chatRoom ->
                            Log.d(TAG, "observeViewModel: ChatRoom $index - ID: ${chatRoom.chatRoomId}, Participants: ${chatRoom.participants}")
                        }

                        chatRoomAdapter.submitList(state.data)
                    }
                }

                is UiState.Failure -> {
                    Log.e(TAG, "observeViewModel: Failure state: ${state.error}")
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewChatRooms.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE

                    val errorMessage = when {
                        state.error.contains("permission", ignoreCase = true) ->
                            "Permission denied. Please check your account permissions."
                        state.error.contains("network", ignoreCase = true) ->
                            "Network error. Please check your internet connection."
                        else -> "Failed to load chat rooms: ${state.error}"
                    }

                    Log.e(TAG, "observeViewModel: Showing error: $errorMessage")
                    motionToastUtil.showFailureToast(requireActivity(), errorMessage)
                    binding.tvEmptyMessage?.text = "Failed to load chats.\nPull down to refresh."
                }
            }
        }
    }

    private fun openChatRoom(chatRoom: ChatRoomModel) {
        Log.d(TAG, "openChatRoom: Opening chat room: ${chatRoom.chatRoomId}")

        val intent = Intent(requireContext(), ChatRoomActivity::class.java).apply {
            putExtra("CHAT_ROOM", chatRoom)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing chat rooms")
        loadChatRooms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}