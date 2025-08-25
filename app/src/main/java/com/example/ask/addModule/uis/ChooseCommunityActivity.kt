package com.example.ask.addModule.uis

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
import com.example.ask.communityModule.adapters.MyCommunityAdapter
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.ActivityChooseCommunityBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class ChooseCommunityActivity : BaseActivity() {

    private lateinit var binding: ActivityChooseCommunityBinding
    private val communityViewModel: CommunityViewModel by viewModels()
    private lateinit var communityAdapter: MyCommunityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_community)

        setupUI()
        setupRecyclerView()
        loadUserCommunities()
        observeViewModel()
    }

    private fun setupUI() {
        binding.toolbarTitle.text = "Choose Community"

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserCommunities()
        }
    }

    private fun setupRecyclerView() {
        communityAdapter = MyCommunityAdapter { community ->
            onCommunitySelected(community)
        }

        binding.recyclerViewCommunities.apply {
            layoutManager = LinearLayoutManager(this@ChooseCommunityActivity)
            adapter = communityAdapter
        }
    }

    private fun onCommunitySelected(community: CommunityModels) {
        val intent = Intent(this, AddQueryActivity::class.java)
        intent.putExtra("selected_community", community)
        intent.putExtra("community_id", community.communityId)
        intent.putExtra("community_name", community.communityName)
        startActivity(intent)
        finish() // Close this activity
    }

    private fun loadUserCommunities() {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            motionToastUtil.showFailureToast(
                this,
                "User not logged in",
                MotionToast.SHORT_DURATION
            )
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        communityViewModel.getUserCommunities(userId)
    }

    private fun observeViewModel() {
        communityViewModel.userCommunities.observe(this) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewCommunities.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        // Show empty state
                        binding.recyclerViewCommunities.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "You haven't joined any communities yet.\nJoin a community first to post queries."
                    } else {
                        binding.recyclerViewCommunities.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        communityAdapter.submitList(state.data)
                    }
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewCommunities.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Failed to load communities.\nPull down to refresh."

                    motionToastUtil.showFailureToast(
                        this,
                        state.error,
                        MotionToast.SHORT_DURATION
                    )
                }
            }
        }
    }
}