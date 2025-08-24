package com.example.ask.mainModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.viewModels.AddViewModel
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

        // Set refresh colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            com.example.ask.R.color.primary_color,
            com.example.ask.R.color.secondary_color
        )
    }

    private fun loadQueries() {
        addViewModel.getAllQueries()
    }

    private fun observeViewModel() {
        addViewModel.allQueries.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewQueries.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        binding.recyclerViewQueries.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.recyclerViewQueries.visibility = View.VISIBLE
                        queryAdapter.submitList(state.data)
                    }
                }

                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewQueries.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE

                    motionToastUtil.showFailureToast(
                        requireActivity(),
                        "Failed to load queries: ${state.error}"
                    )
                }
            }
        }

        // Observe notification sending result
        notificationViewModel.addNotification.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    motionToastUtil.showSuccessToast(
                        requireActivity(),
                        "Help request sent successfully!"
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
        // TODO: Navigate to query details activity
        // For now, show a toast with query info
        motionToastUtil.showInfoToast(
            requireActivity(),
            "Clicked on: ${query.title}"
        )

        // Example of how you might navigate to query details:
        // val intent = Intent(requireContext(), QueryDetailsActivity::class.java)
        // intent.putExtra("query_model", query)
        // startActivity(intent)
    }

    private fun onHelpClicked(query: QueryModel) {
        // Send help notification to the query owner
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
                senderName = currentUser.fullName ?: "Unknown User",
                senderUserId = currentUserId,
                queryId = query.queryId ?: ""
            )
        } else {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "Unable to send help request. Please check your login status."
            )
        }
    }

    private fun onChatClicked(query: QueryModel) {
        // TODO: Navigate to chat activity or open chat
        motionToastUtil.showInfoToast(
            requireActivity(),
            "Chat feature coming soon!"
        )

        // Example implementation:
        // val intent = Intent(requireContext(), ChatActivity::class.java)
        // intent.putExtra("query_id", query.queryId)
        // intent.putExtra("recipient_id", query.userId)
        // startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Queries"
        // Refresh data when fragment becomes visible
        loadQueries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}