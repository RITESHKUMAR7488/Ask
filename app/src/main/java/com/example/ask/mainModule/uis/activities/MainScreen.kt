package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
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

    // ✅ NEW: Add NotificationViewModel
    private val notificationViewModel: NotificationViewModel by viewModels()

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

        // ✅ NEW: Setup notification bell (using your existing btnNotification)
        setupNotificationBell()

        // ✅ NEW: Observe notification count
        observeNotifications()

        // ✅ NEW: Setup back button functionality
        setupBackButton()

        // Default fragment = Home
        replaceFragment(HomeFragment(), "Queries")

        with(binding) {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.Add -> {
                        // ✅ UPDATED: Navigate to ChooseCommunityActivity instead of directly to AddQueryActivity
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
     * ✅ NEW: Setup notification bell click listener (using your existing btnNotification)
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
     * ✅ NEW: Setup back button functionality
     */
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    /**
     * ✅ NEW: Observe notifications and update badge (using your existing notificationBadge)
     */
    private fun observeNotifications() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            // Load unread notification count
            notificationViewModel.getUnreadNotificationCount(userId)

            // Observe unread count
            notificationViewModel.unreadCount.observe(this) { state ->
                when (state) {
                    is UiState.Success -> {
                        updateNotificationBadge(state.data)
                    }
                    is UiState.Failure -> {
                        // Hide badge on error
                        updateNotificationBadge(0)
                    }
                    is UiState.Loading -> {
                        // Keep current state while loading
                    }
                }
            }
        }
    }

    /**
     * ✅ NEW: Update notification badge count (using your existing notificationBadge)
     */
    private fun updateNotificationBadge(count: Int) {
        with(binding) {
            if (count > 0) {
                notificationBadge.visibility = View.VISIBLE
                notificationBadge.text = if (count > 99) "99+" else count.toString()
            } else {
                notificationBadge.visibility = View.GONE
            }
        }
    }

    fun updateToolbarTitle(title: String) {
        binding.toolbarTitle.text = title
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

        // 🔑 Update toolbar title dynamically
        binding.toolbarTitle.text = title
    }

    override fun onResume() {
        super.onResume()
        // ✅ NEW: Refresh notification count when activity resumes
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            notificationViewModel.getUnreadNotificationCount(userId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationViewModel.removeNotificationListener()
    }
}