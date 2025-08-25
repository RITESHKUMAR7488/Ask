package com.example.ask.communityModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.ask.R
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.communityModule.adapters.MyCommunityAdapter
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.uis.Activities.CreateCommunityActivity
import com.example.ask.communityModule.uis.Activities.JoinCommunity
import com.example.ask.communityModule.uis.CommunityActivity
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.FragmentCommunityBinding
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class CommunityFragment : BaseFragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var communityAdapter: MyCommunityAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
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
            showCommunityOptionsBottomSheet()
        }

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserCommunities()
        }
    }

    private fun showCommunityOptionsBottomSheet() {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_community_options, null)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        // Handle Create Community button click
        bottomSheetView.findViewById<View>(R.id.btnCreateCommunity).setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(requireContext(), CreateCommunityActivity::class.java))
        }

        // Handle Join Community button click
        bottomSheetView.findViewById<View>(R.id.btnJoinCommunity).setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(requireContext(), JoinCommunity::class.java))
        }
    }

    private fun loadUserCommunities() {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "User not logged in",
                MotionToast.SHORT_DURATION
            )
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        viewModel.getUserCommunities(userId)
        viewModel.userCommunities.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewCommunities.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    communityAdapter.submitList(state.data)

                    if (state.data.isEmpty()) {
                        // Show empty state
                        binding.recyclerViewCommunities.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewCommunities.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                    }
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewCommunities.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    motionToastUtil.showFailureToast(
                        requireActivity(),
                        state.error,
                        MotionToast.SHORT_DURATION
                    )
                }
            }
        }
    }

    private fun onCommunityClick(community: CommunityModels) {
        val intent = Intent(requireContext(), CommunityActivity::class.java)
        intent.putExtra("community_data", community)
        startActivity(intent)
    }

    private fun showJoinCommunityDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val editText = EditText(requireContext()).apply {
            hint = "Enter community code"
            setPadding(50, 30, 50, 30)
        }

        builder.setTitle("Join Community")
            .setView(editText)
            .setPositiveButton("Join") { _, _ ->
                val code = editText.text.toString().trim()
                if (code.isNotBlank()) {
                    joinCommunity(code)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinCommunity(communityCode: String) {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            motionToastUtil.showFailureToast(
                requireActivity(),
                "User not logged in",
                MotionToast.SHORT_DURATION
            )
            return
        }

        viewModel.joinCommunity(userId, communityCode)
        viewModel.joinCommunity.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showSuccessToast(
                        requireActivity(),
                        "Successfully joined ${state.data.communityName}!",
                        MotionToast.SHORT_DURATION
                    )
                    // Refresh the list
                    loadUserCommunities()
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showFailureToast(
                        requireActivity(),
                        state.error,
                        MotionToast.SHORT_DURATION
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserCommunities()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removeCommunityListener()
        _binding = null
    }
}