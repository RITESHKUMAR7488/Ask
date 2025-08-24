package com.example.ask.notificationModule.uis

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ask.R
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
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notification)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Notifications"
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(this) { notification ->
            onNotificationClicked(notification)
        }

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadNotifications() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            notificationViewModel.getUserNotifications(userId)
        } else {
            showError("User not logged in")
        }
    }

    private fun observeViewModel() {
        notificationViewModel.userNotifications.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    showLoading(true)
                    hideError()
                }

                is UiState.Success -> {
                    showLoading(false)
                    if (state.data.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        notificationAdapter.submitList(state.data)
                        updateUnreadCount(notificationAdapter.getUnreadCount())
                    }
                }

                is UiState.Failure -> {
                    showLoading(false)
                    showError("Failed to load notifications: ${state.error}")
                }
            }
        }
    }

    private fun onNotificationClicked(notification: NotificationModel) {
        // Mark as read if not already read
        if (!notification.isRead) {
            // TODO: Implement mark as read functionality
            // For now, just show toast
            motionToastUtil.showInfoToast(this, "Marking as read...")
        }

        // Handle different notification types
        when (notification.type) {
            "HELP_REQUEST" -> {
                handleHelpRequestNotification(notification)
            }
            "QUERY_UPDATE" -> {
                handleQueryUpdateNotification(notification)
            }
            "COMMUNITY_INVITE" -> {
                handleCommunityInviteNotification(notification)
            }
            "RESPONSE" -> {
                handleResponseNotification(notification)
            }
            else -> {
                motionToastUtil.showInfoToast(
                    this,
                    notification.message ?: "Notification clicked"
                )
            }
        }
    }

    private fun handleHelpRequestNotification(notification: NotificationModel) {
        // Navigate to query details or handle help request
        motionToastUtil.showInfoToast(
            this,
            "Help request from ${notification.senderUserName}"
        )

        // TODO: Navigate to query details
        // val intent = Intent(this, QueryDetailsActivity::class.java)
        // intent.putExtra("query_id", notification.queryId)
        // startActivity(intent)
    }

    private fun handleQueryUpdateNotification(notification: NotificationModel) {
        // Navigate to updated query
        motionToastUtil.showInfoToast(
            this,
            "Query update notification"
        )

        // TODO: Navigate to query details
        // val intent = Intent(this, QueryDetailsActivity::class.java)
        // intent.putExtra("query_id", notification.queryId)
        // startActivity(intent)
    }

    private fun handleCommunityInviteNotification(notification: NotificationModel) {
        // Handle community invitation
        motionToastUtil.showInfoToast(
            this,
            "Community invitation"
        )

        // TODO: Navigate to community details
        // val intent = Intent(this, CommunityDetailsActivity::class.java)
        // intent.putExtra("community_id", notification.communityId)
        // startActivity(intent)
    }

    private fun handleResponseNotification(notification: NotificationModel) {
        // Navigate to query with responses
        motionToastUtil.showInfoToast(
            this,
            "New response on your query"
        )

        // TODO: Navigate to query details with responses
        // val intent = Intent(this, QueryDetailsActivity::class.java)
        // intent.putExtra("query_id", notification.queryId)
        // intent.putExtra("show_responses", true)
        // startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewNotifications.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewNotifications.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewNotifications.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.recyclerViewNotifications.visibility = View.GONE

        motionToastUtil.showFailureToast(this, message)
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun updateUnreadCount(count: Int) {
        // Update toolbar title with unread count
        supportActionBar?.title = if (count > 0) {
            "Notifications ($count unread)"
        } else {
            "Notifications"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationViewModel.removeNotificationListener()
    }

    override fun onResume() {
        super.onResume()
        // Refresh notifications when activity resumes
        loadNotifications()
    }
}