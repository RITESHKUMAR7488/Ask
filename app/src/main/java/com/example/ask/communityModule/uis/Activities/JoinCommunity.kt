package com.example.ask.communityModule.uis.Activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.ask.R
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.ActivityJoinCommunityBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class JoinCommunity : BaseActivity() {

    private lateinit var binding: ActivityJoinCommunityBinding
    private val communityViewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_join_community)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnCreateCommunity.setOnClickListener {
            validateAndJoinCommunity()
        }
    }

    private fun validateAndJoinCommunity() {
        val communityCode = binding.etCommunityCode.text.toString().trim()

        when {
            communityCode.isBlank() -> {
                binding.etCommunityCode.error = "Please enter community code"
                return
            }
            else -> {
                val userId = preferenceManager.userId
                if (userId.isNullOrBlank()) {
                    motionToastUtil.showFailureToast(
                        this,
                        "User is not logged in",
                        duration = MotionToast.SHORT_DURATION
                    )
                    return
                }

                joinCommunity(userId, communityCode)
            }
        }
    }

    private fun joinCommunity(userId: String, communityCode: String) {
        communityViewModel.joinCommunity(userId, communityCode)
    }

    private fun observeViewModel() {
        communityViewModel.joinCommunity.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateCommunity.isEnabled = false
                    binding.btnCreateCommunity.text = "Joining..."
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateCommunity.isEnabled = true
                    binding.btnCreateCommunity.text = "Join Community"

                    motionToastUtil.showSuccessToast(
                        this,
                        "Successfully joined ${state.data.communityName}!",
                        duration = MotionToast.SHORT_DURATION
                    )

                    // Clear the input field
                    binding.etCommunityCode.text?.clear()

                    // Finish activity after successful join
                    finish()
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateCommunity.isEnabled = true
                    binding.btnCreateCommunity.text = "Join Community"

                    motionToastUtil.showFailureToast(
                        this,
                        state.error,
                        duration = MotionToast.SHORT_DURATION
                    )
                }
            }
        }
    }
}