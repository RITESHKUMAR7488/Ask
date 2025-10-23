package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.uis.ChooseCommunityActivity
import com.example.ask.addModule.viewModels.AddViewModel
import com.example.ask.databinding.ActivityMyQueriesBinding
import com.example.ask.mainModule.adapters.QueryAdapter
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyQueriesActivity : BaseActivity() {

    private lateinit var binding: ActivityMyQueriesBinding
    private val addViewModel: AddViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var queryAdapter: QueryAdapter

    companion object {
        private const val TAG = "MyQueriesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_queries)

        setupUI()
        setupRecyclerView()
        setupObservers()
        loadUserQueries()
    }

    private fun setupUI() {
        binding.toolbarTitle.text = "My Queries"

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Add query FAB
        binding.fabAddQuery.setOnClickListener {
            val intent = Intent(this, ChooseCommunityActivity::class.java)
            startActivity(intent)
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserQueries()
        }

        // Set refresh colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.secondary_color
        )
    }

    private fun setupRecyclerView() {
        queryAdapter = QueryAdapter(
            context = this,
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

        binding.recyclerViewMyQueries.apply {
            layoutManager = LinearLayoutManager(this@MyQueriesActivity)
            adapter = queryAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Observe user queries
        addViewModel.userQueries.observe(this) { state ->
            Log.d(TAG, "User queries state: $state")
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewMyQueries.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.fabAddQuery.hide()
                }

                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.fabAddQuery.show()

                    if (state.data.isEmpty()) {
                        showEmptyState()
                    } else {
                        showQueries(state.data)
                    }
                }

                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.fabAddQuery.show()
                    showErrorState(state.error)
                }
            }
        }

        // Observe query status updates
        addViewModel.updateStatus.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // You can show a loading dialog if needed
                }
                is UiState.Success -> {
                    motionToastUtil.showSuccessToast(this, state.data)
                    // Refresh the list to show updated status
                    loadUserQueries()
                }
                is UiState.Failure -> {
                    motionToastUtil.showFailureToast(this, "Failed to update status: ${state.error}")
                }
            }
        }



        // Observe delete query
        addViewModel.deleteQuery.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // You can show a loading dialog if needed
                }
                is UiState.Success -> {
                    motionToastUtil.showSuccessToast(this, state.data)
                    // Refresh the list after successful deletion
                    loadUserQueries()
                }
                is UiState.Failure -> {
                    motionToastUtil.showFailureToast(this, "Failed to delete query: ${state.error}")
                }
            }
        }
    }

    private fun loadUserQueries() {
        val userId = preferenceManager.userId
        Log.d(TAG, "Loading queries for userId: $userId")

        if (!userId.isNullOrEmpty()) {
            addViewModel.getUserQueries(userId)
        } else {
            Log.e(TAG, "User ID is null or empty")
            motionToastUtil.showFailureToast(this, "User not logged in. Please login again.")
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun showEmptyState() {
        binding.recyclerViewMyQueries.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "No Queries Yet"
        binding.tvEmptyMessage.text = "You haven't posted any queries yet.\nTap the + button to create your first query!"
        binding.ivEmptyState.setImageResource(R.drawable.ic_empty_queries)
    }

    private fun showQueries(queries: List<QueryModel>) {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewMyQueries.visibility = View.VISIBLE

        Log.d(TAG, "Displaying ${queries.size} user queries")
        queryAdapter.submitList(queries)

        // Show query statistics
        updateStatistics(queries)
    }

    private fun showErrorState(error: String) {
        binding.recyclerViewMyQueries.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "Failed to Load Queries"
        binding.tvEmptyMessage.text = "Unable to load your queries.\nPull down to refresh."
        binding.ivEmptyState.setImageResource(R.drawable.ic_error)

        motionToastUtil.showFailureToast(this, "Error: $error")
    }

    private fun updateStatistics(queries: List<QueryModel>) {
        val openQueries = queries.count { it.status == "OPEN" }
        val resolvedQueries = queries.count { it.status == "RESOLVED" }
        val totalResponses = queries.sumOf { it.responseCount }

        // Update statistics in toolbar subtitle or dedicated area
        val statsText = "$openQueries open • $resolvedQueries resolved • $totalResponses responses"
        binding.tvStats?.text = statsText
        binding.tvStats?.visibility = View.VISIBLE
    }

    private fun onQueryClicked(query: QueryModel) {
        // Show query options dialog
        showQueryOptionsDialog(query)
    }

    private fun onHelpClicked(query: QueryModel) {
        motionToastUtil.showWarningToast(this, "You cannot request help for your own query")
    }

    private fun onChatClicked(query: QueryModel) {
        motionToastUtil.showInfoToast(this, "Chat feature coming soon!")
    }

    private fun showQueryOptionsDialog(query: QueryModel) {
        val options = when (query.status) {
            "OPEN" -> arrayOf("Mark as In Progress", "Mark as Resolved", "Edit Query", "Delete Query")
            "IN_PROGRESS" -> arrayOf("Mark as Resolved", "Mark as Open", "Edit Query", "Delete Query")
            "RESOLVED" -> arrayOf("Mark as Open", "Reopen Query", "Delete Query")
            "CLOSED" -> arrayOf("Reopen Query", "Delete Query")
            else -> arrayOf("Edit Query", "Delete Query")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Query Options")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Mark as In Progress" -> updateQueryStatus(query.queryId!!, "IN_PROGRESS")
                    "Mark as Resolved" -> updateQueryStatus(query.queryId!!, "RESOLVED")
                    "Mark as Open" -> updateQueryStatus(query.queryId!!, "OPEN")
                    "Mark as Closed" -> updateQueryStatus(query.queryId!!, "CLOSED")
                    "Reopen Query" -> updateQueryStatus(query.queryId!!, "OPEN")
                    "Edit Query" -> editQuery(query)
                    "Delete Query" -> confirmDeleteQuery(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateQueryStatus(queryId: String, newStatus: String) {
        addViewModel.updateQueryStatus(queryId, newStatus)
    }

    private fun editQuery(query: QueryModel) {
        motionToastUtil.showInfoToast(this, "Edit query feature coming soon!")
        // TODO: Implement edit query functionality
        // You can create an EditQueryActivity or reuse AddQueryActivity with edit mode
    }

    private fun confirmDeleteQuery(query: QueryModel) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Query")
            .setMessage("Are you sure you want to delete this query?\n\n\"${query.title}\"\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteQuery(query)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    private fun deleteQuery(query: QueryModel) {
        if (!query.queryId.isNullOrEmpty()) {
            addViewModel.deleteQuery(query.queryId!!)
        } else {
            motionToastUtil.showFailureToast(this, "Unable to delete query - Invalid query ID")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh queries when returning to the activity
        loadUserQueries()
    }
}