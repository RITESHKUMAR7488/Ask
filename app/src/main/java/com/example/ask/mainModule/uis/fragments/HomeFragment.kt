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
import androidx.lifecycle.Observer // Import this
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.uis.CommunityActivity
import com.example.ask.databinding.FragmentHome2Binding // Using the binding from your file
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
import com.example.ask.mainModule.uis.activities.QueryDetailActivity
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    // Using FragmentHome2Binding as seen in your provided file
    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    // These are declared here
    private lateinit var queryAdapter: QueryAdapter
    private lateinit var searchAdapter: SearchResultAdapter

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews() // queryAdapter and searchAdapter are initialized here
        setupSearchView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        // Initialization happens here, before they are used
        queryAdapter = QueryAdapter(
            context = requireContext(),
            onQueryClick = { query ->
                val intent = Intent(requireContext(), QueryDetailActivity::class.java)
                intent.putExtra(QueryDetailActivity.EXTRA_QUERY, query) // Pass the whole object
                startActivity(intent)
            },
            onHelpClick = { query ->
                onHelpClicked(query)
            },
            onChatClick = { query ->
                onChatClicked(query)
            }
        )

        searchAdapter = SearchResultAdapter(requireContext()) { result ->
            handleResultClick(result)
        }

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
    }

    private fun observeViewModel() {
        // --- Coroutine Usage Explanation ---
        // We are using viewLifecycleOwner.lifecycleScope.launch to start a coroutine that is tied
        // to the Fragment's view lifecycle.
        // The repeatOnLifecycle(Lifecycle.State.STARTED) block ensures that
        // the coroutine only collects (listens for) data from the 'uiState' Flow
        // when the Fragment is in the STARTED state (visible).
        // This is good because it prevents the app from doing UI work when
        // the fragment isn't visible, saving resources and preventing crashes.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    // Type argument <List<Any>> is specified
                    updateUiState(state as UiState<List<Any>>)
                }
            }
        }

        // Observe notification VM
        // This observes the new 'addNotificationState' LiveData
        notificationViewModel.addNotificationState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                // We explicitly define the type <String> for Success
                is UiState.Success<String> -> {
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
        })
    }

    private fun updateUiState(state: UiState<List<Any>>) {
        binding.progressBar.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE
        binding.queryRecycler.visibility = View.GONE

        when (state) {
            is UiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            is UiState.Failure -> {
                binding.layoutErrorState.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.error
            }
            // Explicitly define the type <List<Any>> for Success
            is UiState.Success<List<Any>> -> {
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
                            // No unresolved reference, queryAdapter is initialized
                            binding.queryRecycler.adapter = queryAdapter
                            queryAdapter.submitList(state.data as List<QueryModel>)
                        }
                        is SearchResult -> {
                            // No unresolved reference, searchAdapter is initialized
                            binding.queryRecycler.adapter = searchAdapter
                            searchAdapter.submitList(state.data as List<SearchResult>)
                        }
                        else -> {
                            binding.queryRecycler.adapter = queryAdapter
                            queryAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

//    private fun onQueryClicked(query: QueryModel) {
//        motionToastUtil.showInfoToast(
//            requireActivity(),
//            "Clicked on: ${query.title}"
//        )
//        // TODO: Navigate to Query Detail
//    }

    // --- THIS FUNCTION IS FIXED ---
    private fun onHelpClicked(query: QueryModel) {
        val currentUser = preferenceManager.userModel
        val currentUserId = preferenceManager.userId

        // <-- FIX: Changed 'communityID' to 'communityId' (lowercase 'd')
        if (currentUser != null && !currentUserId.isNullOrEmpty() && !query.userId.isNullOrEmpty() && !query.communityId.isNullOrEmpty()) {
            if (currentUserId == query.userId) {
                motionToastUtil.showWarningToast(
                    requireActivity(),
                    "You cannot request help for your own query"
                )
                return
            }

            // This function now has all the required parameters
            notificationViewModel.sendHelpNotification(
                targetUserId = query.userId!!,
                queryTitle = query.title ?: "Unknown Query",
                queryId = query.queryId ?: "",
                communityId = query.communityId!!, // <-- FIX: Changed 'communityID' to 'communityId'
                senderName = currentUser.fullName ?: "Unknown User", // <-- FIX: Changed 'userName' to 'fullName'
                senderUserId = currentUserId,
                senderPhoneNumber = currentUser.mobileNumber,
                senderEmail = currentUser.email,
                senderProfileImage = currentUser.imageUrl // Pass the profile image
            )
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to send help request. User or query data is missing."
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