package com.example.ask.mainModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.uis.CommunityActivity
import com.example.ask.databinding.FragmentHome2Binding
import com.example.ask.databinding.FragmentHomeBinding // Make sure to use fragment_home.xml
import com.example.ask.mainModule.adapters.QueryAdapter
import com.example.ask.mainModule.viewModels.HomeViewModel
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.search.adapters.SearchResultAdapter
import com.example.ask.search.model.SearchResult
import com.example.ask.search.model.SearchResultType
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    // Make sure this binding class matches your XML file name (e.g., fragment_home.xml)
    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    // Use the new HomeViewModel for search and queries
    private val homeViewModel: HomeViewModel by viewModels()

    // ViewModel for sending notifications (from your original file)
    private val notificationViewModel: NotificationViewModel by viewModels()

    private lateinit var queryAdapter: QueryAdapter
    private lateinit var searchAdapter: SearchResultAdapter

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Ensure this layout file 'fragment_home.xml' is the one I provided,
        // which includes the SearchView and state layouts.
        _binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearchView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        // 1. Setup Query Adapter (providing all three required parameters)
        queryAdapter = QueryAdapter(
            context = requireContext(),
            onQueryClick = { query ->
                onQueryClicked(query) // Use your existing handler
            },
            onHelpClick = { query ->
                onHelpClicked(query) // Use your existing handler
            },
            onChatClick = { query ->
                onChatClicked(query) // Use your existing handler
            }
        )

        // 2. Setup Search Adapter
        searchAdapter = SearchResultAdapter(requireContext()) { result ->
            handleResultClick(result)
        }

        // Set up the RecyclerView
        binding.queryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = queryAdapter // Start with query adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                homeViewModel.onQueryChanged(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                homeViewModel.onQueryChanged(newText ?: "")
                return true
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener {
            homeViewModel.retry()
        }

        // The setOnMenuItemClickListener that caused the error has been removed.
    }

    // --- Coroutine Usage ---
    // Safely collects the single UI state from the HomeViewModel.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    updateUiState(state)
                }
            }
        }

        // Observe notification VM (from your original code)
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
                is UiState.Loading -> { /* Loading */ }
            }
        }
    }

    private fun updateUiState(state: UiState<List<Any>>) {
        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE
        binding.queryRecycler.visibility = View.GONE
        // binding.swipeRefreshLayout.isRefreshing = (state is UiState.Loading) // Optional

        when (state) {
            is UiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            is UiState.Failure -> {
                binding.layoutErrorState.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.error
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    // Handle Empty state
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    val isSearching = homeViewModel.searchQuery.value.isNotBlank()
                    if (isSearching) {
                        binding.tvEmptyMessage.text = "No results found for '${homeViewModel.searchQuery.value}'"
                        binding.ivEmptyIcon.setImageResource(R.drawable.ic_search_empty)
                    } else {
                        binding.tvEmptyMessage.text = "No queries found.\nAsk one to get started!"
                        binding.ivEmptyIcon.setImageResource(R.drawable.ic_empty_queries)
                    }
                } else {
                    // We have data, show the recycler
                    binding.queryRecycler.visibility = View.VISIBLE

                    // --- Swap adapter based on data type ---
                    when (state.data.firstOrNull()) {
                        is QueryModel -> {
                            binding.queryRecycler.adapter = queryAdapter
                            queryAdapter.submitList(state.data as List<QueryModel>)
                        }
                        is SearchResult -> {
                            binding.queryRecycler.adapter = searchAdapter
                            searchAdapter.submitList(state.data as List<SearchResult>)
                        }
                        else -> {
                            // Handle case where list is not empty but type is unknown
                            binding.queryRecycler.adapter = queryAdapter
                            queryAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    // --- Click Handlers from your original file ---

    private fun onQueryClicked(query: QueryModel) {
        motionToastUtil.showInfoToast(
            requireActivity(),
            "Clicked on: ${query.title}"
        )
        // TODO: Navigate to Query Detail
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
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to send help request. Please check your login status."
            )
        }
    }

    private fun onChatClicked(query: QueryModel) {
        val currentUserId = preferenceManager.userId

        if (!currentUserId.isNullOrEmpty() && !query.userId.isNullOrEmpty()) {
            if (currentUserId == query.userId) {
                motionToastUtil.showWarningToast(
                    requireActivity(),
                    "You cannot chat with yourself"
                )
                return
            }

            val intent = Intent(requireContext(), ChatRoomActivity::class.java).apply {
                putExtra(ChatRoomActivity.EXTRA_TARGET_USER_ID, query.userId)
                putExtra(ChatRoomActivity.EXTRA_TARGET_USER_NAME, query.userName)
                putExtra(ChatRoomActivity.EXTRA_TARGET_USER_IMAGE, query.userProfileImage)
                putExtra(ChatRoomActivity.EXTRA_QUERY_ID, query.queryId)
                putExtra(ChatRoomActivity.EXTRA_QUERY_TITLE, query.title)
            }
            startActivity(intent)
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to start chat. Please check your login status."
            )
        }
    }

    // --- Click Handler for new Search Results ---

    private fun handleResultClick(result: SearchResult) {
        Log.d(TAG, "Clicked on ${result.type}: ${result.title} (ID: ${result.id})")

        when (result.type) {
            SearchResultType.QUERY -> {
                // TODO: Navigate to Query Detail Screen
                motionToastUtil.showInfoToast(requireActivity(), "Clicked Query: ${result.title}")
            }
            SearchResultType.COMMUNITY -> {
                val communityModel = CommunityModels(communityId = result.id, communityName = result.title)
                val intent = Intent(requireContext(), CommunityActivity::class.java)
                intent.putExtra("community_data", communityModel)
                startActivity(intent)
            }
            SearchResultType.USER -> {
                val intent = Intent(requireContext(), ChatRoomActivity::class.java).apply {
                    putExtra(ChatRoomActivity.EXTRA_TARGET_USER_ID, result.id)
                    putExtra(ChatRoomActivity.EXTRA_TARGET_USER_NAME, result.title)
                    putExtra(ChatRoomActivity.EXTRA_TARGET_USER_IMAGE, result.imageUrl)
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.queryRecycler.adapter = null
        _binding = null
    }
}