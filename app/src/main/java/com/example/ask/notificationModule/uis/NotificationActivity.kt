package com.example.ask.notificationModule.uis

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.ask.R
import com.example.ask.addModule.uis.AddQueryActivity
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.communityModule.uis.CommunityActivity
import com.example.ask.databinding.ActivityNotificationBinding
import com.example.ask.notificationModule.adapters.NotificationAdapter
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder

@AndroidEntryPoint
class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter
    private val TAG = "NotificationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        setupRecyclerView()
        setupObservers()
        viewModel.getUserNotifications() // Load initial data
    }

    private fun setupUI() {
        // Handle loading state
        binding.progressBar.isVisible = true
        binding.swipeRefreshLayout.isRefreshing = false
        binding.layoutEmptyNotification.isVisible = false
        binding.layoutError.isVisible = false
        binding.notificationRecyclerView.isVisible = false
        binding.markAllRead.isVisible = false
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.getUserNotifications()
            binding.swipeRefreshLayout.isRefreshing = true
        }

        binding.markAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            context = this,
            onNotificationClick = { notification ->
                // This is the trigger
                onNotificationClick(notification)
            }
        )
        binding.notificationRecyclerView.adapter = notificationAdapter
    }

    private fun setupObservers() {
        viewModel.userNotifications.observe(this) { state ->
            // Stop refresh indicator regardless of state
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.layoutEmptyNotification.isVisible = false
                    binding.layoutError.isVisible = false
                    binding.notificationRecyclerView.isVisible = false
                    binding.markAllRead.isVisible = false
                }
                is UiState.Success -> {
                    binding.progressBar.isVisible = false
                    val notifications = state.data
                    if (notifications.isNullOrEmpty()) {
                        binding.layoutEmptyNotification.isVisible = true
                        binding.notificationRecyclerView.isVisible = false
                        binding.markAllRead.isVisible = false
                    } else {
                        binding.layoutEmptyNotification.isVisible = false
                        binding.notificationRecyclerView.isVisible = true
                        notificationAdapter.submitList(notifications)
                        val hasUnread = notifications.any { !it.isRead }
                        binding.markAllRead.isVisible = hasUnread
                    }
                    binding.layoutError.isVisible = false
                }
                is UiState.Failure -> {
                    binding.progressBar.isVisible = false
                    binding.layoutError.isVisible = true
                    binding.notificationRecyclerView.isVisible = false
                    binding.layoutEmptyNotification.isVisible = false
                    binding.markAllRead.isVisible = false
                    binding.errorMessage.text = state.error
                    Log.e(TAG, "Error loading notifications: ${state.error}")
                }
            }
        }

        viewModel.markAllAsReadState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.markAllRead.isEnabled = false
                }
                is UiState.Success -> {
                    binding.markAllRead.isEnabled = true
                    motionToastUtil.showSuccessToast(
                        this,
                        "All notifications marked as read."
                    )
                }
                is UiState.Failure -> {
                    binding.markAllRead.isEnabled = true
                    motionToastUtil.showErrorToast(
                        this,
                        state.error ?: "Failed to mark all as read."
                    )
                    Log.e(TAG, "Error marking all as read: ${state.error}")
                }
            }
        }

        viewModel.markAsReadState.observe(this) { state ->
            if (state is UiState.Failure) {
                motionToastUtil.showErrorToast(
                    this,
                    state.error ?: "Failed to mark as read."
                )
                Log.e(TAG, "Failed to mark notification as read: ${state.error}")
            }
        }
    }


    // --- MODIFIED CLICK HANDLER ---
    private fun onNotificationClick(notification: NotificationModel) {
        Log.d(TAG, "Notification clicked: ${notification.notificationId}, Type: ${notification.type}")

        // 1. Mark notification as read
        if (!notification.isRead) {
            val userId = preferenceManager.userId // From BaseActivity
            if (!userId.isNullOrEmpty()) { // Check if userId is not null or empty
                viewModel.markNotificationAsRead(notification.notificationId ?: return)
            } else {
                Log.e(TAG, "User ID is null or empty, cannot mark notification as read.")
            }
        }

        // 2. Decide action: Show contact dialog or navigate
        if (!notification.senderPhone.isNullOrBlank()) {
            // NEW FEATURE: Show contact dialog
            showContactOptionsDialog(notification)
        } else {
            // OLD FEATURE: Navigate directly
            navigateToNotificationAction(notification)
        }
    }

    // --- NEW FUNCTION: TO SHOW THE DIALOG ---
    private fun showContactOptionsDialog(notification: NotificationModel) {
        val phoneNumber = notification.senderPhone ?: return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact_options, null)

        // This constructor uses the theme set in themes.xml, fixing the unresolved style error
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val whatsappButton = dialogView.findViewById<LinearLayout>(R.id.layoutWhatsapp)
        val callButton = dialogView.findViewById<LinearLayout>(R.id.layoutCall)

        whatsappButton.setOnClickListener {
            openWhatsApp(phoneNumber, notification)
            dialog.dismiss()
        }

        callButton.setOnClickListener {
            openDialer(phoneNumber)
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- NEW FUNCTION: WHATSAPP INTENT ---
    private fun openWhatsApp(phoneNumber: String, notification: NotificationModel) {
        try {
            // --- THIS IS THE FIX ---
            // Get current user's name from the userModel in PreferenceManager
            val currentUserName = preferenceManager.userModel?.fullName ?: "a user"
            // -----------------------

            val queryMessage = notification.message ?: "your query"
            val message = getString(R.string.whatsapp_message_prefill, currentUserName, queryMessage)

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMessage")

            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "WhatsApp not installed.", e)
            motionToastUtil.showErrorToast(this, getString(R.string.whatsapp_not_installed))
        } catch (e: Exception) {
            Log.e(TAG, "Could not open WhatsApp.", e)
            motionToastUtil.showErrorToast(this, getString(R.string.intent_error))
        }
    }

    // --- NEW FUNCTION: DIALER INTENT ---
    private fun openDialer(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open dialer.", e)
            motionToastUtil.showErrorToast(this, getString(R.string.intent_error))
        }
    }

    // --- REFACTORED: OLD LOGIC MOVED HERE ---
    private fun navigateToNotificationAction(notification: NotificationModel) {
        when (notification.type) {
            "HELP_REQUEST",
            "QUERY_UPDATE",
            "RESPONSE" -> {
                if (notification.queryId.isNullOrEmpty() || notification.communityId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing queryId or communityId for notification type ${notification.type}")
                    showNavigationErrorToast()
                    return
                }
                val intent = Intent(this, AddQueryActivity::class.java)
                intent.putExtra("queryID", notification.queryId)
                intent.putExtra("communityID", notification.communityId)
                intent.putExtra("from", "notification")
                startActivity(intent)
            }

            "COMMUNITY_INVITE",
            "COMMUNITY_JOIN" -> {
                if (notification.communityId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing communityId for notification type ${notification.type}")
                    showNavigationErrorToast()
                    return
                }
                val intent = Intent(this, CommunityActivity::class.java)
                intent.putExtra("communityID", notification.communityId)
                startActivity(intent)
            }

            "CHAT" -> {
                if (notification.queryId.isNullOrEmpty() || notification.senderUserId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing chatRoomId (in queryId) or senderUserId for CHAT notification")
                    showNavigationErrorToast()
                    return
                }
                val intent = Intent(this, ChatRoomActivity::class.java)
                intent.putExtra("chatRoomId", notification.queryId) // Assuming queryId holds chatRoomId
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_ID, notification.senderUserId)
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_NAME, notification.senderUserName)
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_IMAGE, notification.senderProfileImage)
                startActivity(intent)
            }

            else -> {
                Log.w(TAG, "Unknown notification type clicked: ${notification.type}")
                motionToastUtil.showInfoToast(this, "This notification has no specific action.")
            }
        }
    }

    private fun showNavigationErrorToast() {
        motionToastUtil.showErrorToast(
            this,
            "Could not open notification. Data is missing."
        )
    }
}