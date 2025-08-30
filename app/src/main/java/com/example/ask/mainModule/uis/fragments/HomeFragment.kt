package com.example.ask.mainModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.viewModels.AddViewModel
import com.example.ask.chatModule.ui.ChatActivity
import com.example.ask.databinding.FragmentHome2Binding
import com.example.ask.mainModule.adapters.QueryAdapter
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    private val addViewModel: AddViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var queryAdapter: QueryAdapter

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        loadQueries()
        debugCommunityData()
    }

    private fun setupRecyclerView() {
        queryAdapter = QueryAdapter(
            context = requireContext(),
            onQueryClick = { query ->
                onQueryClicked(query)
            },
            onHelpClick = { query ->
                onHelpClicked(query)
            },
            onChatClick = { query ->
                onChatClicked(query)
            }
        )

        binding.recyclerViewQueries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = queryAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadQueries()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            com.example.ask.R.color.primary_color,
            com.example.ask.R.color.secondary_color
        )
    }

    private fun loadQueries() {
        val userId = preferenceManager.userId
        Log.d(TAG, "loadQueries: userId = $userId")

        if (!userId.isNullOrEmpty()) {
            Log.d(TAG, "loadQueries: Calling getQueriesFromUserCommunities for userId: $userId")
            addViewModel.getQueriesFromUserCommunities(userId)
        } else {
            Log.e(TAG, "loadQueries: userId is null or empty")
            motionToastUtil.showFailureToast(
                requireActivity(),
                "User not logged in. Please login to view queries."
            )
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        addViewModel.communityQueries.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "observeViewModel: Received state: $state")
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    Log.d(TAG, "observeViewModel: Loading state")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewQueries.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                is UiState.Success -> {
                    Log.d(TAG, "observeViewModel: Success state with ${state.data.size} queries")
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        Log.d(TAG, "observeViewModel: No queries found, showing empty state")
                        binding.recyclerViewQueries.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyMessage?.text = "No queries found in your joined communities.\nJoin communities to see queries or create your first query!"
                    } else {
                        Log.d(TAG, "observeViewModel: Showing ${state.data.size} queries")
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewQueries.visibility = View.VISIBLE

                        // Log each query for debugging
                        state.data.forEachIndexed { index, query ->
                            Log.d(TAG, "observeViewModel: Query $index - Title: ${query.title}, Community: ${query.communityName}, CommunityId: ${query.communityId}")
                        }

                        queryAdapter.submitList(state.data)
                    }
                }

                is UiState.Failure -> {
                    Log.e(TAG, "observeViewModel: Failure state: ${state.error}")
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewQueries.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE

                    val errorMessage = when {
                        state.error.contains("permission", ignoreCase = true) ->
                            "Permission denied. Please check your account permissions."
                        state.error.contains("network", ignoreCase = true) ->
                            "Network error. Please check your internet connection."
                        else -> "Failed to load queries from your communities: ${state.error}"
                    }

                    Log.e(TAG, "observeViewModel: Showing error: $errorMessage")
                    motionToastUtil.showFailureToast(requireActivity(), errorMessage)
                    binding.tvEmptyMessage?.text = "Failed to load queries.\nPull down to refresh."
                }
            }
        }

        notificationViewModel.addNotification.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    motionToastUtil.showSuccessToast(
                        requireActivity(),
                        "Help request sent successfully! 🤝"
                    )
                }
                is UiState.Failure -> {
                    motionToastUtil.showFailureToast(
                        requireActivity(),
                        "Failed to send help request: ${state.error}"
                    )
                }
                is UiState.Loading -> {
                    // Show loading if needed
                }
            }
        }
    }

    private fun onQueryClicked(query: QueryModel) {
        motionToastUtil.showInfoToast(
            requireActivity(),
            "Clicked on: ${query.title}"
        )
    }

    private fun onHelpClicked(query: QueryModel) {
        val currentUser = preferenceManager.userModel
        val currentUserId = preferenceManager.userId

        if (currentUser != null && !currentUserId.isNullOrEmpty() && !query.userId.isNullOrEmpty()) {
            if (currentUserId == query.userId) {
                motionToastUtil.showWarningToast(
                    requireActivity(),
                    "You cannot request help for your own query"
                )
                return
            }

            notificationViewModel.sendHelpNotification(
                targetUserId = query.userId!!,
                queryTitle = query.title ?: "Unknown Query",
                queryId = query.queryId ?: "",
                senderName = currentUser.fullName ?: "Unknown User",
                senderUserId = currentUserId,
                senderPhoneNumber = currentUser.mobileNumber,
                senderEmail = currentUser.email
            )

            motionToastUtil.showInfoToast(
                requireActivity(),
                "Sending help request to ${query.userName}..."
            )
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to send help request. Please check your login status."
            )
        }
    }

    private fun onChatClicked(query: QueryModel) {
        val currentUserId = preferenceManager.userId

        if (!query.userId.isNullOrEmpty()) {
            if (currentUserId == query.userId) {
                motionToastUtil.showWarningToast(
                    requireActivity(),
                    "You cannot chat with yourself"
                )
                return
            }

            // ✅ Launch ChatActivity with query information
            ChatActivity.startChatActivity(
                context = requireContext(),
                targetUserId = query.userId!!,
                targetUserName = query.userName,
                targetUserAvatar = query.userProfileImage,
                queryTitle = query.title
            )
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to start chat. User information not available."
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Community Queries"
        Log.d(TAG, "onResume: Refreshing queries")
        loadQueries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun debugCommunityData() {
        val userId = preferenceManager.userId
        Log.d(TAG, "debugCommunityData: Starting debug for userId = $userId")

        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "debugCommunityData: userId is null/empty")
            return
        }

        val database = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // 1. Check if user document exists
        database.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                Log.d(TAG, "debugCommunityData: User document exists: ${userDoc.exists()}")
                Log.d(TAG, "debugCommunityData: User document data: ${userDoc.data}")

                // 2. Check MyCommunity subcollection
                database.collection("users")
                    .document(userId)
                    .collection("MyCommunity")
                    .get()
                    .addOnSuccessListener { communityDocs ->
                        Log.d(TAG, "debugCommunityData: Found ${communityDocs.size()} community documents")

                        communityDocs.documents.forEach { doc ->
                            Log.d(TAG, "debugCommunityData: Community doc ID: ${doc.id}")
                            Log.d(TAG, "debugCommunityData: Community doc data: ${doc.data}")
                            val communityId = doc.getString("communityId")
                            val communityName = doc.getString("communityName")
                            Log.d(TAG, "debugCommunityData: Extracted - communityId: $communityId, communityName: $communityName")
                        }

                        // 3. Check all posts in the posts collection
                        database.collection("posts").get()
                            .addOnSuccessListener { postDocs ->
                                Log.d(TAG, "debugCommunityData: Total posts in database: ${postDocs.size()}")

                                postDocs.documents.forEach { doc ->
                                    val title = doc.getString("title")
                                    val communityId = doc.getString("communityId")
                                    val communityName = doc.getString("communityName")
                                    Log.d(TAG, "debugCommunityData: Post - Title: $title, CommunityId: $communityId, CommunityName: $communityName")
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "debugCommunityData: Failed to get posts", exception)
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "debugCommunityData: Failed to get user communities", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "debugCommunityData: Failed to get user document", exception)
            }
    }
}