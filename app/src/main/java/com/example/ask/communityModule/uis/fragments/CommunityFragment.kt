package com.example.ask.communityModule.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.communityModule.adapters.MyCommunityAdapter
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.FragmentCommunityBinding
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Example: observe communities
        val userId = "someUserId" // replace with actual logged-in user id
        viewModel.getUserCommunities(userId)

        viewModel.userCommunities.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    // show shimmer or progress bar
                }
                is UiState.Success -> {
                    val adapter = MyCommunityAdapter { community ->

                    }
                    binding.recyclerViewCommunities.layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerViewCommunities.adapter = adapter
                    adapter.submitList(state.data)
                }

                is UiState.Failure -> {
                    // show error message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removeCommunityListener() // ✅ cleanup
        _binding = null
    }
}
