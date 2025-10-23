package com.example.ask.notificationModule.uis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.ask.addModule.uis.AddQueryActivity
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.communityModule.uis.CommunityActivity
import com.example.ask.databinding.ActivityNotificationBinding
import com.example.ask.notificationModule.adapters.NotificationAdapter
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

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
            // Keep swipeRefreshLayout.isRefreshing = true
            // The observer will set it to false when data arrives
            binding.swipeRefreshLayout.isRefreshing = true
        }

        binding.markAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
        }
    }

    private fun setupRecyclerView() {
        // FIX: Correctly initialize NotificationAdapter with context and onNotificationClick
        notificationAdapter = NotificationAdapter(
            context = this,
            onNotificationClick = { notification ->
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
                        // FIX: Use 'isRead' field from NotificationModel
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
                    // FIX: Use 'error' field from UiState.Failure
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
                    // FIX: Use 'motionToastUtil' from BaseActivity
                    motionToastUtil.showSuccessToast(
                        this,
                        "All notifications marked as read."
                    )
                }
                is UiState.Failure -> {
                    binding.markAllRead.isEnabled = true
                    // FIX: Use 'motionToastUtil' and 'error' field
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
                // FIX: Use 'motionToastUtil' and 'error' field
                motionToastUtil.showErrorToast(
                    this,
                    state.error ?: "Failed to mark as read."
                )
                Log.e(TAG, "Failed to mark notification as read: ${state.error}")
            }
            // Success is handled implicitly by the userNotifications listener refreshing the list
        }
    }

    private fun onNotificationClick(notification: NotificationModel) {
        Log.d(TAG, "Notification clicked: ${notification.notificationId}, Type: ${notification.type}")

        // 1. Mark notification as read
        // FIX: Use 'isRead' field and get userId from preferenceManager
        if (!notification.isRead) {
            val userId = preferenceManager.userId
            if (!notification.isRead) {
                // The ViewModel already knows the userId, so just pass the notificationId
                viewModel.markNotificationAsRead(notification.notificationId ?: return)
            }else {
                Log.e(TAG, "User ID is null, cannot mark notification as read.")
            }
        }

        // 2. Navigate based on notification type
        // Note: Check NotificationUtils.kt for the exact strings you used.
        when (notification.type) {
            "HELP_REQUEST",
            "QUERY_UPDATE",
            "RESPONSE" -> {
                // FIX: Use correct fields 'queryId' and 'communityId'
                if (notification.queryId.isNullOrEmpty() || notification.communityId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing queryId or communityId for notification type ${notification.type}")
                    showNavigationErrorToast()
                    return
                }
                // FIX: Correct Intent creation
                val intent = Intent(this, AddQueryActivity::class.java)
                intent.putExtra("queryID", notification.queryId)
                intent.putExtra("communityID", notification.communityId)
                intent.putExtra("from", "notification")
                startActivity(intent)
            }

            "COMMUNITY_INVITE",
            "COMMUNITY_JOIN" -> {
                // FIX: Use correct field 'communityId'
                if (notification.communityId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing communityId for notification type ${notification.type}")
                    showNavigationErrorToast()
                    return
                }
                // FIX: Correct Intent creation
                val intent = Intent(this, CommunityActivity::class.java)
                intent.putExtra("communityID", notification.communityId)
                startActivity(intent)
            }

            "CHAT" -> {
                // FIX: Use correct fields 'queryId'(as chatRoomId), 'senderUserId', 'senderUserName', 'senderProfileImage'
                if (notification.queryId.isNullOrEmpty() || notification.senderUserId.isNullOrEmpty()) {
                    Log.w(TAG, "Missing chatRoomId (in queryId) or senderUserId for CHAT notification")
                    showNavigationErrorToast()
                    return
                }
                // FIX: Correct Intent creation and use ChatRoomActivity constants
                val intent = Intent(this, ChatRoomActivity::class.java)
                intent.putExtra("chatRoomId", notification.queryId) // Assuming queryId holds chatRoomId
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_ID, notification.senderUserId)
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_NAME, notification.senderUserName)
                intent.putExtra(ChatRoomActivity.EXTRA_TARGET_USER_IMAGE, notification.senderProfileImage)
                startActivity(intent)
            }

            else -> {
                Log.w(TAG, "Unknown notification type clicked: ${notification.type}")
                // FIX: Use 'motionToastUtil'
                motionToastUtil.showInfoToast(this, "This notification has no specific action.")
            }
        }
    }

    private fun showNavigationErrorToast() {
        // FIX: Use 'motionToastUtil'
        motionToastUtil.showErrorToast(
            this,
            "Could not open notification. Data is missing."
        )
    }
}