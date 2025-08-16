package com.example.ask.communityModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.communityModule.adapters.MyCommunityAdapter
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.uis.Activities.CreateCommunityActicity
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.FragmentCommunityBinding
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class CommunityFragment : BaseFragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModel()
    private lateinit var communityAdapter: MyCommunityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeUserCommunities()
        loadUserCommunities()
    }

    private fun setupRecyclerView() {
        communityAdapter = MyCommunityAdapter { community ->
            onCommunityClick(community)
        }

        binding.recyclerViewCommunities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = communityAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAdd.setOnClickListener {
            val intent = Intent(requireContext(), CreateCommunityActicity::class.java)
            startActivity(intent)
        }
    }

    private fun observeUserCommunities() {
        viewModel.userCommunities.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    showLoading(true)
                }
                is UiState.Success -> {
                    showLoading(false)
                    handleSuccessState(state.data)
                }
                is UiState.Failure -> {
                    showLoading(false)
                    showErrorToast(state.error)
                }
            }
        }
    }

    private fun loadUserCommunities() {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            showErrorToast("User not logged in")
            return
        }
        viewModel.getUserCommunities(userId)
    }

    private fun handleSuccessState(communities: List<CommunityModels>) {
        if (communities.isEmpty()) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
            communityAdapter.submitList(communities)
        }
    }

    private fun onCommunityClick(community: CommunityModels) {
        // Handle community item click
        // You can navigate to community details or perform other actions
        showInfoToast("Clicked on: ${community.communityName}")
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewCommunities.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        // Assuming you have empty state views in your layout
        // If not, you can remove this or add them to your layout
        binding.recyclerViewCommunities.visibility = if (show) View.GONE else View.VISIBLE
        // binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showErrorToast(error: String) {
        motionToastUtil.showFailureToast(
            requireActivity(),
            error,
            MotionToast.SHORT_DURATION
        )
    }

    private fun showInfoToast(message: String) {
        motionToastUtil.showInfoToast(
            requireActivity(),
            message,
            MotionToast.SHORT_DURATION
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh communities when fragment becomes visible
        loadUserCommunities()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop listening to Firestore updates to prevent memory leaks
        viewModel.removeCommunityListener()
        _binding = null
    }
}