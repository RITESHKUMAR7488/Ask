package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.ask.R
import com.example.ask.addModule.uis.ChooseCommunityActivity
import com.example.ask.communityModule.uis.fragments.CommunityFragment
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.mainModule.uis.fragments.HomeFragment
import com.example.ask.notificationModule.uis.NotificationActivity
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.onBoardingModule.uis.FirstScreen
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainScreen : BaseActivity() {
    private lateinit var binding: ActivityMainScreenBinding
    private val notificationViewModel: NotificationViewModel by viewModels()

    companion object {
        private const val TAG = "MainScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in before setting up the main screen
        if (!isUserLoggedIn()) {
            redirectToFirstScreen()
            return
        }

        // User is logged in, proceed with main screen setup
        setupMainScreen()
    }

    private fun isUserLoggedIn(): Boolean {
        return preferenceManager.isLoggedIn &&
                preferenceManager.userModel != null &&
                !preferenceManager.userId.isNullOrEmpty()
    }

    private fun redirectToFirstScreen() {
        val intent = Intent(this, FirstScreen::class.java)
        startActivity(intent)
        finish() // Close this activity so user can't go back with back button
    }

    private fun setupMainScreen() {
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_screen)

        // Setup notification bell
        setupNotificationBell()

        // Observe notification count
        observeNotifications()

        // Setup back button functionality
        setupBackButton()

        // Default fragment = Home
        replaceFragment(HomeFragment(), "Queries")

        with(binding) {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.Add -> {
                        val intent = Intent(this@MainScreen, ChooseCommunityActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.community -> {
                        replaceFragment(CommunityFragment(), "Communities")
                        true
                    }
                    else -> {
                        replaceFragment(HomeFragment(), "Queries")
                        true
                    }
                }
            }
        }
    }

    /**
     * Setup notification bell click listener
     */
    private fun setupNotificationBell() {
        binding.btnNotification.setOnClickListener {
            // Navigate to NotificationActivity
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)

            // Show feedback
            motionToastUtil.showInfoToast(this, "Opening notifications...")
        }
    }

    /**
     * Setup back button functionality
     */
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    /**
     * Observe notifications and update badge
     */
    private fun observeNotifications() {
        val userId = preferenceManager.userId
        Log.d(TAG, "observeNotifications: userId = $userId")

        if (!userId.isNullOrEmpty()) {
            // Load unread notification count
            notificationViewModel.getUnreadNotificationCount(userId)

            // Observe unread count
            notificationViewModel.unreadCount.observe(this) { state ->
                Log.d(TAG, "Notification state received: $state")
                when (state) {
                    is UiState.Success -> {
                        Log.d(TAG, "Unread notification count: ${state.data}")
                        updateNotificationBadge(state.data)
                    }
                    is UiState.Failure -> {
                        Log.e(TAG, "Failed to get unread count: ${state.error}")
                        // Hide badge on error
                        updateNotificationBadge(0)
                    }
                    is UiState.Loading -> {
                        Log.d(TAG, "Loading notification count...")
                        // Keep current state while loading
                    }
                }
            }
        } else {
            Log.w(TAG, "UserId is null or empty, cannot observe notifications")
        }
    }

    /**
     * Update notification badge count with improved visibility
     */
    private fun updateNotificationBadge(count: Int) {
        Log.d(TAG, "updateNotificationBadge called with count: $count")

        with(binding) {
            if (count > 0) {
                Log.d(TAG, "Showing badge with count: $count")
                notificationBadge.visibility = View.VISIBLE
                notificationBadge.text = if (count > 99) "99+" else count.toString()

                // Force visibility and bring to front
                notificationBadge.bringToFront()

                // Test with a temporary toast to confirm the method is being called
                motionToastUtil.showInfoToast(this@MainScreen, "Badge updated: $count notifications")

            } else {
                Log.d(TAG, "Hiding badge (count is 0)")
                notificationBadge.visibility = View.GONE
            }
        }

        // Force layout refresh
        binding.notificationContainer.invalidate()
        binding.notificationContainer.requestLayout()
    }

    /**
     * Test method to manually trigger badge update - you can call this for testing
     */
    fun testNotificationBadge() {
        Log.d(TAG, "Testing notification badge...")
        updateNotificationBadge(5) // Test with 5 notifications

        // Also test hiding after 3 seconds
        binding.notificationBadge.postDelayed({
            updateNotificationBadge(0)
        }, 3000)
    }

    fun updateToolbarTitle(title: String) {
        binding.toolbarTitle.text = title
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

        // Update toolbar title dynamically
        binding.toolbarTitle.text = title
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // Refresh notification count when activity resumes
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            Log.d(TAG, "Refreshing notification count for user: $userId")
            notificationViewModel.getUnreadNotificationCount(userId)
        }

        // TEMPORARY: Test badge functionality
        // Remove this line once you confirm it's working
        // testNotificationBadge()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        notificationViewModel.removeNotificationListener()
    }
}