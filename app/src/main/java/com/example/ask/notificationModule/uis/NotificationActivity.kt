package com.example.ask.notificationModule.uis

import android.content.Intent
import android.net.Uri
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
        setupSwipeRefresh()
        setupErrorRetry()
        observeViewModel()
        loadNotifications()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // ✅ FIXED: Implement mark all as read functionality
        binding.btnMarkAllRead.setOnClickListener {
            markAllNotificationsAsRead()
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

    // ✅ NEW: Setup error retry button
    private fun setupErrorRetry() {
        binding.btnRetry?.setOnClickListener {
            loadNotifications()
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

    // ✅ NEW: Implement mark all as read
    private fun markAllNotificationsAsRead() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            val unreadCount = notificationAdapter.getUnreadCount()
            if (unreadCount > 0) {
                // Show confirmation dialog
                android.app.AlertDialog.Builder(this)
                    .setTitle("Mark All as Read")
                    .setMessage("Mark all $unreadCount unread notifications as read?")
                    .setPositiveButton("Yes") { _, _ ->
                        notificationViewModel.markAllNotificationsAsRead(userId)
                        notificationAdapter.markAllAsRead()
                        motionToastUtil.showSuccessToast(this, "All notifications marked as read")
                        updateUnreadCount(0)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                motionToastUtil.showInfoToast(this, "No unread notifications")
            }
        }
    }

    private fun observeViewModel() {
        notificationViewModel.userNotifications.observe(this) { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    showLoading(true)
                    hideError()
                    hideEmptyState()
                }

                is UiState.Success -> {
                    showLoading(false)
                    hideError()

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
                    hideEmptyState()
                    showError("Failed to load notifications: ${state.error}")
                }
            }
        }

        // ✅ NEW: Observe mark as read result
        notificationViewModel.markAsRead.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    // Notification marked as read successfully
                    // The adapter will update automatically through the real-time listener
                }
                is UiState.Failure -> {
                    motionToastUtil.showFailureToast(this, "Failed to mark as read: ${state.error}")
                }
                else -> {}
            }
        }
    }

    private fun onNotificationClicked(notification: NotificationModel) {
        // Mark as read if not already read
        if (!notification.isRead) {
            val userId = preferenceManager.userId
            if (!userId.isNullOrEmpty()) {
                notificationViewModel.markNotificationAsRead(userId, notification.notificationId ?: "")
            }
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
     * ✅ IMPROVED: Better contact options with more robust parsing
     */
    private fun handleHelpRequestNotification(notification: NotificationModel) {
        val message = notification.message ?: ""

        // Extract phone number and email with improved regex
        val phoneRegex = Regex("📞 Phone: ([+]?[0-9\\s()-]{7,15})")
        val emailRegex = Regex("📧 Email: ([\\w._%+-]+@[\\w.-]+\\.[A-Z|a-z]{2,})")

        val phoneNumber = phoneRegex.find(message)?.groupValues?.get(1)?.trim()?.replace(Regex("[\\s()-]"), "")
        val email = emailRegex.find(message)?.groupValues?.get(1)?.trim()

        val queryTitle = extractQueryTitle(message)

        // Show contact options
        showContactOptionsDialog(
            senderName = notification.senderUserName ?: "Someone",
            phoneNumber = phoneNumber,
            email = email,
            queryTitle = queryTitle
        )
    }

    /**
     * ✅ IMPROVED: Extract query title from message
     */
    private fun extractQueryTitle(message: String): String {
        // Try to extract from patterns like "with 'Title'" or "about 'Title'"
        val titleRegex = Regex("(?:with|about) '([^']+)'")
        return titleRegex.find(message)?.groupValues?.get(1) ?: "your query"
    }

    /**
     * ✅ IMPROVED: Enhanced contact options dialog
     */
    private fun showContactOptionsDialog(
        senderName: String,
        phoneNumber: String?,
        email: String?,
        queryTitle: String
    ) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        // Add phone options
        if (!phoneNumber.isNullOrEmpty()) {
            options.add("📞 Call $phoneNumber")
            actions.add { makePhoneCall(phoneNumber) }

            options.add("💬 WhatsApp")
            actions.add { openWhatsApp(phoneNumber) }
        }

        // Add email option
        if (!email.isNullOrEmpty()) {
            options.add("📧 Email $email")
            actions.add { sendEmail(email, "Help with: $queryTitle") }
        }

        if (options.isEmpty()) {
            motionToastUtil.showInfoToast(this, "$senderName wants to help with: $queryTitle")
            return
        }

        // Create options dialog
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Contact $senderName")
        builder.setMessage("$senderName offered to help with: $queryTitle\n\nChoose how to contact them:")

        builder.setItems(options.toTypedArray()) { _, which ->
            try {
                actions[which].invoke()
            } catch (e: Exception) {
                motionToastUtil.showFailureToast(this, "Failed to open contact method")
            }
        }

        builder.setNegativeButton("Later") { dialog, _ ->
            dialog.dismiss()
            motionToastUtil.showInfoToast(this, "You can contact $senderName anytime!")
        }

        builder.show()
    }

    /**
     * ✅ IMPROVED: Better phone call handling
     */
    private fun makePhoneCall(phoneNumber: String) {
        try {
            // Clean phone number (remove spaces, brackets, etc.)
            val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")

            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                motionToastUtil.showSuccessToast(this, "Opening dialer...")
            } else {
                motionToastUtil.showFailureToast(this, "No dialer app found")
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "Cannot make phone call: ${e.message}")
        }
    }

    /**
     * ✅ IMPROVED: Better WhatsApp handling
     */
    private fun openWhatsApp(phoneNumber: String) {
        try {
            // Clean and format phone number for WhatsApp
            val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
            val formattedNumber = if (cleanNumber.startsWith("+")) {
                cleanNumber.substring(1) // Remove + for WhatsApp
            } else {
                cleanNumber
            }

            // Try WhatsApp intent first
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedNumber")
                setPackage("com.whatsapp")
            }

            if (whatsappIntent.resolveActivity(packageManager) != null) {
                startActivity(whatsappIntent)
                motionToastUtil.showSuccessToast(this, "Opening WhatsApp...")
            } else {
                // Fallback to web WhatsApp
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber")
                }
                if (webIntent.resolveActivity(packageManager) != null) {
                    startActivity(webIntent)
                    motionToastUtil.showInfoToast(this, "Opening WhatsApp Web...")
                } else {
                    motionToastUtil.showFailureToast(this, "WhatsApp not available")
                }
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "Cannot open WhatsApp: ${e.message}")
        }
    }

    /**
     * ✅ IMPROVED: Better email handling
     */
    private fun sendEmail(email: String, subject: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Hi! I saw your offer to help with my query. Thanks for reaching out!")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Send Email"))
                motionToastUtil.showSuccessToast(this, "Opening email app...")
            } else {
                motionToastUtil.showFailureToast(this, "No email app found")
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "Cannot send email: ${e.message}")
        }
    }

    private fun handleQueryUpdateNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "Query update: ${notification.message}")

        // TODO: Navigate to query details
        // val intent = Intent(this, QueryDetailsActivity::class.java)
        // intent.putExtra("query_id", notification.queryId)
        // startActivity(intent)
    }

    private fun handleCommunityInviteNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "Community invitation: ${notification.message}")

        // TODO: Navigate to community details
        // val intent = Intent(this, CommunityDetailsActivity::class.java)
        // intent.putExtra("community_id", notification.communityId)
        // startActivity(intent)
    }

    private fun handleResponseNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "New response: ${notification.message}")

        // TODO: Navigate to query details with responses
        // val intent = Intent(this, QueryDetailsActivity::class.java)
        // intent.putExtra("query_id", notification.queryId)
        // intent.putExtra("show_responses", true)
        // startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.recyclerViewNotifications.visibility = View.GONE
            hideEmptyState()
            hideError()
        }
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewNotifications.visibility = View.GONE
        hideError()
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewNotifications.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        // Check if error layout exists in the XML
        binding.layoutError?.let { errorLayout ->
            errorLayout.visibility = View.VISIBLE
            binding.tvError?.text = message
            binding.recyclerViewNotifications.visibility = View.GONE
            hideEmptyState()
        }

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