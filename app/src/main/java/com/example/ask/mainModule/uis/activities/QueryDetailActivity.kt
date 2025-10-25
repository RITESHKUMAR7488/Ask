package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.chatModule.uis.activities.ChatRoomActivity
import com.example.ask.databinding.ActivityQueryDetailBinding
import com.example.ask.notificationModule.utils.NotificationUtils // For time formatting
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class QueryDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityQueryDetailBinding
    private var query: QueryModel? = null
    private var currentUserId: String? = null

    companion object {
        const val EXTRA_QUERY = "EXTRA_QUERY"
        const val EXTRA_QUERY_ID = "EXTRA_QUERY_ID" // For receiving ID from search
        private const val TAG = "QueryDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_query_detail)
        currentUserId = preferenceManager.userId

        setupToolbar()
        getQueryDataFromIntent()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun getQueryDataFromIntent() {
        // Check if the full QueryModel object was passed
        query = intent.getParcelableExtra(EXTRA_QUERY)

        if (query != null) {
            bindQueryData(query!!)
        } else {
            // If not, check if just the queryId was passed (e.g., from search)
            val queryId = intent.getStringExtra(EXTRA_QUERY_ID)
            if (queryId != null) {
                // TODO: Fetch the full QueryModel from Firestore using the queryId
                // For now, show a loading/error state or minimal info
                motionToastUtil.showInfoToast(this, "Loading query details...")
                // In a real app, you'd use a ViewModel to fetch details by ID here.
                // fetchQueryDetailsById(queryId)
                binding.tvQueryTitle.text = "Loading..."
                // Disable actions until data loads
                binding.fabHelp.visibility = View.GONE
                binding.ivChatAction.visibility = View.GONE

            } else {
                motionToastUtil.showErrorToast(this, "Could not load query details.")
                finish() // Close activity if no data found
            }
        }
    }

    // Placeholder for fetching details if only ID is passed
    // private fun fetchQueryDetailsById(queryId: String) {
    //     // Use a ViewModel and Repository to fetch QueryModel from Firestore
    //     // Once fetched, call bindQueryData(fetchedQuery)
    // }

    private fun bindQueryData(queryData: QueryModel) {
        this.query = queryData // Store the loaded query

        binding.toolbar.title = queryData.title ?: "Query Details"

        // User Info
        binding.tvUserName.text = queryData.userName ?: "Unknown User"
        queryData.timestamp?.let {
            binding.tvTimestamp.text = "Posted ${NotificationUtils.getTimeAgo(it)}"
        }
        Glide.with(this)
            .load(queryData.userProfileImage)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .into(binding.ivUserProfile)

        // Query Details
        binding.tvQueryTitle.text = queryData.title
        binding.tvQueryDescription.text = queryData.description
        binding.tvLocation.text = queryData.location ?: "No location specified"
        binding.tvCommunityName.text = queryData.communityName ?: "General"

        // Image
        if (!queryData.imageUrl.isNullOrEmpty()) {
            binding.ivQueryImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(queryData.imageUrl)
                .placeholder(R.drawable.image_placeholder_background)
                .into(binding.ivQueryImage)
        } else {
            binding.ivQueryImage.visibility = View.GONE
        }

        // Chips
        binding.chipCategory.text = queryData.category
        setStatusUI(queryData.status ?: "OPEN")
        setPriorityUI(queryData.priority ?: "NORMAL")

        // Tags
        if (!queryData.tags.isNullOrEmpty()) {
            binding.tvTags.visibility = View.VISIBLE
            binding.tvTags.text = queryData.tags?.joinToString(", ") { "#$it" }
        } else {
            binding.tvTags.visibility = View.GONE
        }

        // --- Action Buttons Logic ---
        // Hide/Show FAB and Chat based on whether it's the user's own query
        if (queryData.userId == currentUserId) {
            binding.fabHelp.visibility = View.GONE // Can't help own query
            binding.ivChatAction.visibility = View.GONE // Can't chat with self
        } else {
            // Show help button only if query is open/in progress
            binding.fabHelp.visibility = if (queryData.status == "OPEN" || queryData.status == "IN_PROGRESS") View.VISIBLE else View.GONE
            binding.ivChatAction.visibility = View.VISIBLE
        }
    }

    private fun setStatusUI(status: String) {
        binding.chipStatus.text = status
        val colorRes = when (status) {
            "OPEN" -> R.color.status_open_color
            "IN_PROGRESS" -> R.color.status_progress_color
            "RESOLVED" -> R.color.success_color
            "CLOSED" -> R.color.status_closed_color
            else -> R.color.status_default
        }
        binding.chipStatus.setChipBackgroundColorResource(colorRes)
    }

    private fun setPriorityUI(priority: String) {
        binding.chipPriority.text = priority
        val colorRes = when (priority) {
            "HIGH" -> R.color.error_color
            "NORMAL" -> R.color.primary_color
            "LOW" -> R.color.gray_500
            else -> R.color.gray_400
        }
        binding.chipPriority.setChipBackgroundColorResource(colorRes)
    }


    private fun setupClickListeners() {
        binding.fabHelp.setOnClickListener {
            // TODO: Implement "Offer Help" functionality (like sending notification)
            motionToastUtil.showInfoToast(this, "Offer Help clicked!")
            // You can reuse the logic from HomeFragment's onHelpClicked here
        }

        binding.ivChatAction.setOnClickListener {
            startChat()
        }
    }

    private fun startChat() {
        if (query == null || query?.userId == currentUserId) {
            motionToastUtil.showWarningToast(this, "Cannot start chat.")
            return
        }

        val intent = Intent(this, ChatRoomActivity::class.java).apply {
            putExtra(ChatRoomActivity.EXTRA_TARGET_USER_ID, query?.userId)
            putExtra(ChatRoomActivity.EXTRA_TARGET_USER_NAME, query?.userName)
            putExtra(ChatRoomActivity.EXTRA_TARGET_USER_IMAGE, query?.userProfileImage)
            putExtra(ChatRoomActivity.EXTRA_QUERY_ID, query?.queryId) // Pass query info
            putExtra(ChatRoomActivity.EXTRA_QUERY_TITLE, query?.title)
        }
        startActivity(intent)
    }
}