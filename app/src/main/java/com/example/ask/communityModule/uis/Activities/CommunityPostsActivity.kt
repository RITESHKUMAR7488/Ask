package com.example.ask.communityModule.uis.Activities// Or a more suitable package

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.databinding.ActivityCommunityPostsBinding
import com.example.ask.mainModule.adapters.QueryAdapter
import com.example.ask.mainModule.uis.activities.QueryDetailActivity
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import com.example.ask.communityModule.viewModels.CommunityPostsViewModel // Create this ViewModel

@AndroidEntryPoint
class CommunityPostsActivity : BaseActivity() {

    private lateinit var binding: ActivityCommunityPostsBinding
    private val viewModel: CommunityPostsViewModel by viewModels()
    private lateinit var queryAdapter: QueryAdapter
    private var communityId: String? = null
    private var communityName: String? = null

    companion object {
        const val EXTRA_COMMUNITY_ID = "EXTRA_COMMUNITY_ID"
        const val EXTRA_COMMUNITY_NAME = "EXTRA_COMMUNITY_NAME"
        private const val TAG = "CommunityPostsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_posts)

        communityId = intent.getStringExtra(EXTRA_COMMUNITY_ID)
        communityName = intent.getStringExtra(EXTRA_COMMUNITY_NAME)

        if (communityId == null) {
            motionToastUtil.showErrorToast(this, "Community ID not found.")
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        loadPosts()
    }

    private fun setupToolbar() {
        binding.toolbarTitle.text = communityName ?: "Community Posts"
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        queryAdapter = QueryAdapter(
            context = this,
            onQueryClick = { query ->
                // Navigate to Query Detail
                val intent = Intent(this, QueryDetailActivity::class.java).apply {
                    putExtra(QueryDetailActivity.EXTRA_QUERY, query)
                }
                startActivity(intent)
            },
            onHelpClick = { query ->
                // Handle help click (e.g., send notification)
                motionToastUtil.showInfoToast(this, "Help clicked for: ${query.title}")
                // You might reuse the logic from HomeFragment's onHelpClicked here
            },
            onChatClick = { query ->
                // Handle chat click
                motionToastUtil.showInfoToast(this, "Chat clicked for: ${query.title}")
                // You might reuse the logic from HomeFragment's onChatClicked here
            }
        )
        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(this@CommunityPostsActivity)
            adapter = queryAdapter
        }
    }

    private fun setupObservers() {
        // --- Coroutine Usage ---
        // The ViewModel uses viewModelScope internally to launch coroutines
        // for fetching data. This observer simply receives the final state (Loading, Success, Failure)
        // on the main thread, ensuring UI updates are safe.
        viewModel.postsState.observe(this, Observer { state ->
            binding.swipeRefreshLayout.isRefreshing = false // Stop refreshing indicator
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewPosts.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.data.isEmpty()) {
                        binding.recyclerViewPosts.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "No posts found in this community yet."
                    } else {
                        binding.recyclerViewPosts.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        queryAdapter.submitList(state.data)
                    }
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewPosts.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Failed to load posts: ${state.error}"
                    motionToastUtil.showErrorToast(this, "Error: ${state.error}")
                }
            }
        })
    }


    private fun loadPosts() {
        communityId?.let {
            binding.swipeRefreshLayout.isRefreshing = true // Show refreshing indicator
            viewModel.fetchPostsForCommunity(it)
        }
    }
}