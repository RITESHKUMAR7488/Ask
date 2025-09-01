package com.example.ask.notificationModule.uis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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

    private fun markAllNotificationsAsRead() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            val unreadCount = notificationAdapter.getUnreadCount()
            if (unreadCount > 0) {
                androidx.appcompat.app.AlertDialog.Builder(this)
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

        notificationViewModel.markAsRead.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    // Notification marked as read successfully
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

        // Add haptic feedback
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
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
        val message = notification.message ?: ""

        // Enhanced regex patterns for better contact extraction
        val phoneRegex = Regex("📞\\s*(?:Phone:|Call:)?\\s*([+]?[0-9\\s()-]{7,15})")
        val emailRegex = Regex("📧\\s*(?:Email:|Mail:)?\\s*([\\w._%+-]+@[\\w.-]+\\.[A-Z|a-z]{2,})")

        val phoneNumber = phoneRegex.find(message)?.groupValues?.get(1)?.trim()?.replace(Regex("[\\s()-]"), "")
        val email = emailRegex.find(message)?.groupValues?.get(1)?.trim()

        val queryTitle = extractQueryTitle(message)

        // Validate extracted contact info
        val validPhone = if (isValidPhoneNumber(phoneNumber)) phoneNumber else null
        val validEmail = if (isValidEmail(email)) email else null

        // Show contact options
        showContactOptionsDialog(
            senderName = notification.senderUserName ?: "Helper",
            phoneNumber = validPhone,
            email = validEmail,
            queryTitle = queryTitle
        )
    }

    private fun showContactOptionsDialog(
        senderName: String,
        phoneNumber: String?,
        email: String?,
        queryTitle: String
    ) {
        val options = mutableListOf<Pair<String, () -> Unit>>()

        // Add available contact options with better labels
        if (!phoneNumber.isNullOrEmpty()) {
            options.add("📞 Call $senderName" to {
                makeEnhancedPhoneCall(phoneNumber, senderName, queryTitle)
            })

            options.add("💬 WhatsApp $senderName" to {
                openEnhancedWhatsApp(phoneNumber, senderName, queryTitle)
            })
        }

        if (!email.isNullOrEmpty()) {
            options.add("📧 Email $senderName" to {
                sendEnhancedEmail(email, senderName, queryTitle)
            })
        }

        if (options.isEmpty()) {
            motionToastUtil.showInfoToast(this, "🤝 $senderName wants to help with: $queryTitle")
            return
        }

        // Create enhanced options dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🤝 Contact $senderName")

        val dialogMessage = "💡 $senderName offered to help with:\n\"$queryTitle\"\n\nChoose how to contact them:"
        builder.setMessage(dialogMessage)

        val optionTexts = options.map { it.first }.toTypedArray()
        builder.setItems(optionTexts) { dialog, which ->
            try {
                options[which].second.invoke()
            } catch (e: Exception) {
                motionToastUtil.showFailureToast(this, "❌ Failed to open contact method")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Maybe Later") { dialog, _ ->
            dialog.dismiss()
            motionToastUtil.showInfoToast(this, "💾 You can contact $senderName anytime from notifications!")
        }

        // Add copy info option
        if (!phoneNumber.isNullOrEmpty() || !email.isNullOrEmpty()) {
            builder.setNeutralButton("📋 Copy Info") { _, _ ->
                val contactInfo = buildString {
                    append("$senderName's Contact Info:\n")
                    if (!phoneNumber.isNullOrEmpty()) append("📞 Phone: $phoneNumber\n")
                    if (!email.isNullOrEmpty()) append("📧 Email: $email\n")
                    append("\nQuery: $queryTitle")
                }
                copyToClipboard(contactInfo, "$senderName's Contact Info")
            }
        }

        val dialog = builder.create()

        // Customize dialog button colors
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                ContextCompat.getColor(this, R.color.primary_color)
            )
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(this, R.color.gray_600)
            )
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
                ContextCompat.getColor(this, R.color.secondary_color)
            )
        }

        dialog.show()
    }

    private fun makeEnhancedPhoneCall(phoneNumber: String, senderName: String, queryTitle: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")

            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                motionToastUtil.showSuccessToast(this, "📞 Calling $senderName...")

                // Show helpful reminder after delay
                Handler(Looper.getMainLooper()).postDelayed({
                    motionToastUtil.showInfoToast(this, "💡 Remember: Asking about \"$queryTitle\"")
                }, 2000)
            } else {
                showManualContactDialog(senderName, phoneNumber, "Phone")
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "❌ Cannot make call: ${e.message}")
        }
    }

    private fun openEnhancedWhatsApp(phoneNumber: String, senderName: String, queryTitle: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
            val formattedNumber = if (cleanNumber.startsWith("+")) {
                cleanNumber.substring(1)
            } else {
                cleanNumber
            }

            // Enhanced WhatsApp message with context
            val message = "Hi $senderName! 👋\n\nI saw your offer to help with: \"$queryTitle\"\n\nThanks for reaching out! When would be a good time to discuss this?"
            val encodedMessage = Uri.encode(message)

            // Try WhatsApp intent first
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedNumber?text=$encodedMessage")
                setPackage("com.whatsapp")
            }

            if (whatsappIntent.resolveActivity(packageManager) != null) {
                startActivity(whatsappIntent)
                motionToastUtil.showSuccessToast(this, "💬 Opening WhatsApp with $senderName...")
            } else {
                // Fallback to web WhatsApp
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$formattedNumber?text=$encodedMessage")
                }
                if (webIntent.resolveActivity(packageManager) != null) {
                    startActivity(webIntent)
                    motionToastUtil.showInfoToast(this, "💬 Opening WhatsApp Web...")
                } else {
                    showManualContactDialog(senderName, phoneNumber, "WhatsApp")
                }
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "❌ Cannot open WhatsApp: ${e.message}")
        }
    }

    private fun sendEnhancedEmail(email: String, senderName: String, queryTitle: String) {
        try {
            val subject = "Help Request: $queryTitle"
            val body = """
Hi $senderName,

Thank you for offering to help with my query: "$queryTitle"

I saw your notification and would appreciate your assistance. Please let me know:
1. When would be a convenient time to discuss this?
2. What additional information do you need from my side?
3. What's the best way to proceed?

Looking forward to hearing from you!

Best regards
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Send Email to $senderName"))
                motionToastUtil.showSuccessToast(this, "📧 Opening email app...")
            } else {
                showManualContactDialog(senderName, email, "Email")
            }
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "❌ Cannot send email: ${e.message}")
        }
    }

    private fun showManualContactDialog(senderName: String, contactInfo: String, contactType: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📱 Manual Contact")
            .setMessage("$contactType app not available on your device.\n\n$senderName's $contactType:\n$contactInfo\n\nTap 'Copy' to copy this information.")
            .setPositiveButton("📋 Copy") { _, _ ->
                copyToClipboard(contactInfo, "$senderName's $contactType")
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun copyToClipboard(text: String, label: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            motionToastUtil.showSuccessToast(this, "📋 $label copied to clipboard!")
        } catch (e: Exception) {
            motionToastUtil.showFailureToast(this, "❌ Failed to copy to clipboard")
        }
    }

    private fun extractQueryTitle(message: String): String {
        // Try multiple patterns to extract query title
        val patterns = listOf(
            Regex("(?:with|about|regarding)\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE),
            Regex("(?:query:|question:|help with:)\\s*['\"]?([^'\"\\n]+)['\"]?", RegexOption.IGNORE_CASE),
            Regex("help.*?['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE),
            Regex("\\*\\*([^*]+)\\*\\*"), // Bold text pattern
            Regex("'([^']+)'") // Simple single quotes
        )

        for (pattern in patterns) {
            val match = pattern.find(message)   // ✅ no RegexOption here
            if (match != null) {
                val title = match.groupValues[1].trim()
                if (title.length > 3) {
                    return if (title.length > 50) {
                        title.take(47) + "..."
                    } else {
                        title
                    }
                }
            }
        }

        // Fallback
        val meaningfulWords = message
            .replace(Regex("[📞📧💬📱]"), "") // Remove emoji
            .split(" ")
            .filter { word ->
                word.length > 2 &&
                        !word.contains("@") &&
                        !word.matches(Regex(".*[0-9]{3,}.*"))
            }

        return if (meaningfulWords.size > 3) {
            meaningfulWords.take(6).joinToString(" ") + "..."
        } else {
            "your query"
        }
    }


    private fun isValidPhoneNumber(phone: String?): Boolean {
        if (phone.isNullOrEmpty()) return false
        val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
        return cleanPhone.length >= 7 && cleanPhone.length <= 15
    }

    private fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrEmpty()) return false
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun handleQueryUpdateNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "Query update: ${notification.message}")
        // TODO: Navigate to query details
    }

    private fun handleCommunityInviteNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "Community invitation: ${notification.message}")
        // TODO: Navigate to community details
    }

    private fun handleResponseNotification(notification: NotificationModel) {
        motionToastUtil.showInfoToast(this, "New response: ${notification.message}")
        // TODO: Navigate to query details with responses
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
        loadNotifications()
    }
}