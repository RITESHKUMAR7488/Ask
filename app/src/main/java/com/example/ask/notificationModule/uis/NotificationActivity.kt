package com.example.ask.notificationModule.uis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
        setupSwipeRefresh()
        observeViewModel()
        loadNotifications()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnMarkAllRead.setOnClickListener {
            // TODO: Implement mark all as read functionality
            motionToastUtil.showInfoToast(this, "Mark all as read - Coming soon!")
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.secondary_color
        )
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
            binding.swipeRefreshLayout.isRefreshing = false

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
            notificationViewModel.markNotificationAsRead(notification.notificationId ?: "")
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

    /**
     * ✅ NEW: Handle help request notification with contact options
     */
    private fun handleHelpRequestNotification(notification: NotificationModel) {
        val message = notification.message ?: ""

        // Extract phone number and email from message if available
        val phoneRegex = Regex("📞 Phone: ([+\\d\\s()-]+)")
        val emailRegex = Regex("📧 Email: ([\\w._%+-]+@[\\w.-]+\\.[A-Z|a-z]{2,})")

        val phoneNumber = phoneRegex.find(message)?.groupValues?.get(1)?.trim()
        val email = emailRegex.find(message)?.groupValues?.get(1)?.trim()

        // Show options dialog
        showContactOptionsDialog(
            senderName = notification.senderUserName ?: "Someone",
            phoneNumber = phoneNumber,
            email = email,
            queryTitle = notification.message?.substringAfter("with '")?.substringBefore("'") ?: "your query"
        )
    }

    private fun showContactOptionsDialog(
        senderName: String,
        phoneNumber: String?,
        email: String?,
        queryTitle: String
    ) {
        val options = mutableListOf<String>()

        if (!phoneNumber.isNullOrEmpty()) {
            options.add("📞 Call $phoneNumber")
            options.add("💬 WhatsApp $phoneNumber")
        }

        if (!email.isNullOrEmpty()) {
            options.add("📧 Email $email")
        }

        if (options.isEmpty()) {
            motionToastUtil.showInfoToast(this, "$senderName wants to help with: $queryTitle")
            return
        }

        // Create a simple options dialog
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("$senderName wants to help!")
        builder.setMessage("Choose how to contact them about: $queryTitle")

        builder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                options.find { it.startsWith("📞 Call") } -> {
                    phoneNumber?.let { makePhoneCall(it) }
                }
                options.find { it.startsWith("💬 WhatsApp") } -> {
                    phoneNumber?.let { openWhatsApp(it) }
                }
                options.find { it.startsWith("📧 Email") } -> {
                    email?.let { sendEmail(it, "Help with: $queryTitle") }
                }
            }
        }

        builder.setNegativeButton("Later") { dialog, _ ->
            dialog.dismiss()
            motionToastUtil.showInfoToast(this, "You can contact $senderName later")
        }

        builder.show()
    }

    private fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "Cannot make phone call")
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$cleanNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "WhatsApp not installed")
        }
    }

    private fun sendEmail(email: String, subject: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Hi! I saw your offer to help with my query. Thanks!")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "No email app found")
        }
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
        binding.layoutError?.visibility = View.VISIBLE
        binding.tvError?.text = message
        binding.recyclerViewNotifications.visibility = View.GONE

        motionToastUtil.showFailureToast(this, message)
    }

    private fun hideError() {
        binding.layoutError?.visibility = View.GONE
    }

    private fun updateUnreadCount(count: Int) {
        // Update toolbar title with unread count
        binding.toolbarTitle.text = if (count > 0) {
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