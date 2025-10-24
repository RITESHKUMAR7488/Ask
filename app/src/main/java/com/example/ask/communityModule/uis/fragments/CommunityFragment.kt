package com.example.ask.communityModule.uis.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
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
        observeViewModel()
        loadUserCommunities()
    }

    private fun setupRecyclerView() {
        communityAdapter = MyCommunityAdapter { community ->
            showCommunityOptionsBottomSheet(community)
        }

        binding.recyclerViewCommunities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = communityAdapter
        }
    }

    private fun setupClickListeners() {
        // ✅ Updated: Show bottom sheet options when add button is clicked
        binding.btnAdd.setOnClickListener {
            showAddCommunityOptionsBottomSheet()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserCommunities()
        }
    }

    // ✅ New method: Show bottom sheet with Create/Join options
    private fun showAddCommunityOptionsBottomSheet() {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_community_options, null)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        val btnCreateCommunity = bottomSheetView.findViewById<View>(R.id.btnCreateCommunity)
        val btnJoinCommunity = bottomSheetView.findViewById<View>(R.id.btnJoinCommunity)

        btnCreateCommunity.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = Intent(requireContext(), CreateCommunityActivity::class.java)
            startActivity(intent)
        }

        btnJoinCommunity.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = Intent(requireContext(), JoinCommunity::class.java)
            startActivity(intent)
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
    }

    private fun observeViewModel() {
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

        viewModel.leaveCommunity.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showSuccessToast(
                        requireActivity(),
                        state.data,
                        MotionToast.SHORT_DURATION
                    )
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

        viewModel.deleteCommunity.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    motionToastUtil.showSuccessToast(
                        requireActivity(),
                        state.data,
                        MotionToast.SHORT_DURATION
                    )
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

    private fun showCommunityOptionsBottomSheet(community: CommunityModels) {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_community_actions, null)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        val btnView = bottomSheetView.findViewById<View>(R.id.btnViewCommunity)
        val btnLeave = bottomSheetView.findViewById<View>(R.id.btnLeaveCommunity)
        val btnDelete = bottomSheetView.findViewById<View>(R.id.btnDeleteCommunity)
        val btnCopyCode = bottomSheetView.findViewById<View>(R.id.btnCopyCode)
        val btnCancel = bottomSheetView.findViewById<View>(R.id.btnCancel)

        // Show/hide based on role
        when (community.role?.lowercase()) {
            "admin" -> {
                btnDelete.visibility = View.VISIBLE
                btnCopyCode.visibility = View.VISIBLE
                btnLeave.visibility = View.GONE
            }
            "member" -> {
                btnLeave.visibility = View.VISIBLE
                btnDelete.visibility = View.GONE
                btnCopyCode.visibility = View.GONE
            }
            else -> {
                btnLeave.visibility = View.GONE
                btnDelete.visibility = View.GONE
                btnCopyCode.visibility = View.GONE
            }
        }

        btnView.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = Intent(requireContext(), CommunityActivity::class.java)
            intent.putExtra("community_data", community)
            startActivity(intent)
        }

        btnLeave.setOnClickListener {
            bottomSheetDialog.dismiss()
            showLeaveCommunityConfirmation(community)
        }

        btnDelete.setOnClickListener {
            bottomSheetDialog.dismiss()
            showDeleteCommunityConfirmation(community)
        }

        // ✅ CORRECTED: Use communityCode instead of communityId
        btnCopyCode.setOnClickListener {
            bottomSheetDialog.dismiss()
            community.communityCode?.let { code ->
                copyCommunityCodeToClipboard(code)
            }
        }

        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
    }

    // ✅ New helper function to copy text to clipboard
    private fun copyCommunityCodeToClipboard(code: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Community Code", code)
        clipboard.setPrimaryClip(clip)

        motionToastUtil.showSuccessToast(
            requireActivity(),
            "Community code copied to clipboard",
            MotionToast.SHORT_DURATION
        )
    }

    private fun showLeaveCommunityConfirmation(community: CommunityModels) {
        AlertDialog.Builder(requireContext())
            .setTitle("Leave Community")
            .setMessage("Are you sure you want to leave ${community.communityName}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.leaveCommunity(preferenceManager.userId ?: "", community.communityId ?: "")
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteCommunityConfirmation(community: CommunityModels) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Community")
            .setMessage("Are you sure you want to delete ${community.communityName}? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteCommunity(preferenceManager.userId ?: "", community.communityId ?: "")
            }
            .setNegativeButton("No", null)
            .show()
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