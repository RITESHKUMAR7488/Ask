package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.addModule.uis.ChooseCommunityActivity
import com.example.ask.communityModule.uis.fragments.CommunityFragment
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.mainModule.uis.fragments.HomeFragment
import com.example.ask.mainModule.uis.activities.MyQueriesActivity
import com.example.ask.notificationModule.uis.NotificationActivity
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.onBoardingModule.uis.FirstScreen
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainScreen : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainScreenBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle

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

        // Setup navigation drawer
        setupNavigationDrawer()

        // Setup navigation header
        setupNavigationHeader()

        // Setup back press handling
        setupBackPressHandling()

        // Setup notification bell
        setupNotificationBell()

        // Observe notification count
        observeNotifications()

        // Setup menu button functionality
        setupMenuButton()

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
     * Setup back press handling using OnBackPressedDispatcher
     */
    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Let the system handle the back press (finish activity, etc.)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    /**
     * Setup navigation drawer
     */
    private fun setupNavigationDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Fix spacing issue for specific devices
        fixNavigationViewSpacing()
    }

    /**
     * Fix navigation view spacing programmatically for device-specific issues
     */
    private fun fixNavigationViewSpacing() {
        try {
            // Force remove any system-added margins/padding
            binding.navigationView.setPadding(0, 0, 0, 0)

            // Get the NavigationMenuView (internal view that holds menu items)
            val navigationMenuView = binding.navigationView.getChildAt(0) as? android.widget.ListView
            navigationMenuView?.let { menuView ->
                menuView.setPadding(0, 0, 0, 0)

                // Set divider height to 0 if needed
                menuView.dividerHeight = 0

                // Force layout update
                menuView.requestLayout()
            }

            // Alternative approach - find RecyclerView if NavigationView uses it
            val recyclerView = findRecyclerViewInNavigationView(binding.navigationView)
            recyclerView?.let { rv ->
                rv.setPadding(0, 0, 0, 0)
                rv.requestLayout()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fixing navigation spacing: ${e.message}")
        }
    }

    /**
     * Helper method to find RecyclerView in NavigationView
     */
    private fun findRecyclerViewInNavigationView(view: View): androidx.recyclerview.widget.RecyclerView? {
        if (view is androidx.recyclerview.widget.RecyclerView) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = findRecyclerViewInNavigationView(view.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }

    /**
     * Setup navigation header with user info
     */
    private fun setupNavigationHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val ivProfileImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivProfileImage)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)

        // Load user data from preferences
        val userModel = preferenceManager.userModel
        userModel?.let { user ->
            // Set user name
            tvUserName.text = user.fullName ?: "User Name"

            // Set user email
            tvUserEmail.text = user.email ?: "user@example.com"

            // Load profile image
            if (!user.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(user.imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivProfileImage)
            } else {
                ivProfileImage.setImageResource(R.drawable.ic_person)
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
     * Setup menu button functionality
     */
    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    /**
     * Handle navigation drawer item selection
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(HomeFragment(), "Queries")
                // Update bottom navigation selection
                binding.bottomNavigationView.selectedItemId = R.id.home
            }
            R.id.nav_my_queries -> {
                // ✅ Navigate to MyQueriesActivity
                val intent = Intent(this, MyQueriesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_communities -> {
                replaceFragment(CommunityFragment(), "Communities")
                // Update bottom navigation selection
                binding.bottomNavigationView.selectedItemId = R.id.community
            }
            R.id.nav_notifications -> {
                val intent = Intent(this, NotificationActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_profile -> {
                // TODO: Navigate to Profile activity
                motionToastUtil.showInfoToast(this, "Profile - Coming Soon!")
            }
            R.id.nav_settings -> {
                // TODO: Navigate to Settings activity
                motionToastUtil.showInfoToast(this, "Settings - Coming Soon!")
            }
            R.id.nav_about -> {
                // TODO: Show About dialog/activity
                motionToastUtil.showInfoToast(this, "About - Coming Soon!")
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }

        // Close drawer after selection
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform logout operation
     */
    private fun performLogout() {
        // Clear user data from preferences
        preferenceManager.clearUserData()

        // Show success message
        motionToastUtil.showSuccessToast(this, "Logged out successfully")

        // Navigate to FirstScreen
        val intent = Intent(this, FirstScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

            } else {
                Log.d(TAG, "Hiding badge (count is 0)")
                notificationBadge.visibility = View.GONE
            }
        }

        // Force layout refresh
        binding.notificationContainer.invalidate()
        binding.notificationContainer.requestLayout()
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

        // Refresh navigation header in case user data changed
        setupNavigationHeader()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        notificationViewModel.removeNotificationListener()
    }
}