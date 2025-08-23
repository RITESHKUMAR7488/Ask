package com.example.ask.communityModule.uis.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.example.ask.MainModule.uis.activities.MainScreen
import com.example.ask.R
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.viewModels.CommunityViewModel
import com.example.ask.databinding.ActivityCreateCommunityActicityBinding
import com.example.ask.databinding.DialogCommunityCodeBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast
import java.util.UUID

@AndroidEntryPoint
class CreateCommunityActicity : BaseActivity() {

    private lateinit var binding: ActivityCreateCommunityActicityBinding
    private val communityViewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_community_acticity)

        with(binding) {
            btnCreateCommunity.setOnClickListener {
                validateAndCreateCommunity()
            }
        }
    }

    // ✅ FIXED: Following RegisterActivity validation pattern
    private fun validateAndCreateCommunity() {
        val communityName = binding.etCommunityName.text.toString().trim()

        when {
            communityName.isBlank() -> {
                binding.etCommunityName.error = "Please Enter Community Name"
            }
            else -> {
                val userId = preferenceManager.userId
                if (userId.isNullOrBlank()) {
                    motionToastUtil.showFailureToast(
                        this,
                        "User is Not Logged in",
                        duration = MotionToast.SHORT_DURATION
                    )
                    return
                }

                val communityModel = CommunityModels(
                    communityName = communityName,
                    userId = userId,
                    communityCode = generateCommunityCode()
                )

                // ✅ FIXED: Following RegisterActivity observer pattern
                communityViewModel.addCommunity(userId, communityModel, role = "admin")
                communityViewModel.addCommunity.observe(this) { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnCreateCommunity.isEnabled = false
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCreateCommunity.isEnabled = true
                            showCommunityCodeDialog(state.data)
                        }
                        is UiState.Failure -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCreateCommunity.isEnabled = true
                            motionToastUtil.showFailureToast(
                                this@CreateCommunityActicity,
                                state.error,
                                duration = MotionToast.SHORT_DURATION
                            )
                            Log.e("CreateCommunity", "Failed: ${state.error}")
                        }
                    }
                }
            }
        }
    }

    private fun generateCommunityCode(): String {
        val words = listOf(
            "spark", "ohm", "volta", "ampere", "watt", "tesla", "coil", "charge",
            "photon", "circuit", "neutron", "flux", "phase", "current", "pulse", "wave",
            "quantum", "resistor", "capacitor", "inductor", "diode", "relay", "plasma", "glow",
            "arc", "field", "node", "grid", "power", "signal", "amp", "zenon"
        )

        val randomWords = words.shuffled().take(6)
        return randomWords.joinToString("-").uppercase()
    }

    private fun showCommunityCodeDialog(communityModel: CommunityModels) {
        val dialogBinding = DataBindingUtil.inflate<DialogCommunityCodeBinding>(
            LayoutInflater.from(this),
            R.layout.dialog_community_code,
            null,
            false
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        with(dialogBinding) {
            tvCommunityName.text = communityModel.communityName
            tvCommunityCode.text = communityModel.communityCode

            btnClose.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            btnCopy.setOnClickListener {
                copyToClipboard(communityModel.communityCode ?: "")
            }

            btnDone.setOnClickListener {
                dialog.dismiss()
                finish()
            }
        }

        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Community Code", text)
        clipboardManager.setPrimaryClip(clipData)

        motionToastUtil.showSuccessToast(
            this,
            "Community code copied to clipboard",
            duration = MotionToast.SHORT_DURATION
        )
    }
}
