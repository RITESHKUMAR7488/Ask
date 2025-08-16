package com.example.ask.communityModule.uis.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.communityModule.ui.CreateCommunityActivity
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.FragmentCommunityBinding
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommunityFragment : BaseFragment() {

    private lateinit var binding: FragmentCommunityBinding
    private val viewModel: CommunityViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)

        // Setup RecyclerView
        binding.recyclerViewCommunities.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CommunityAdapter(mutableListOf()) // <-- Adapter should take CommunityModels
        binding.recyclerViewCommunities.adapter = adapter

        // Observe Live Updates (Communities list)
        viewModel.getUserCommunities("USER_ID_HERE").observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.updateList(state.data) // refresh adapter
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(state.error.toString())
                }
            }
        }

        // Add button click -> go to CreateCommunity
        binding.btnAddCommunity.setOnClickListener {
            val intent = Intent(requireContext(), CreateCommunityActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}
