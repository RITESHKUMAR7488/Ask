package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.cometchat.chat.models.User
import com.example.ask.MyApplication
import com.example.ask.R
import com.example.ask.addModule.uis.ChooseCommunityActivity
import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.chatModule.ui.ChatActivity
import com.example.ask.communityModule.uis.fragments.CommunityFragment
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.mainModule.uis.fragments.HomeFragment
import com.example.ask.notificationModule.uis.NotificationActivity
import com.example.ask.notificationModule.viewModels.NotificationViewModel
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.onBoardingModule.uis.FirstScreen
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainScreen : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainScreenBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle

    @Inject
    lateinit var cometChatManager: CometChatManager

    companion object {
        private const val TAG = "MainScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If user is not logged in -> go to onboarding
        if (!isUserLoggedIn()) {
            redirectToFirstScreen()
            return
        }

        // ALWAYS setup main screen first to initialize binding
        setupMainScreen()

        // Then handle CometChat initialization asynchronously
        initializeCometChatAsync()
    }

    private fun isUserLoggedIn(): Boolean {
        return preferenceManager.isLoggedIn &&
                preferenceManager.userModel != null &&
                !preferenceManager.userId.isNullOrEmpty()
    }

    private fun redirectToFirstScreen() {
        startActivity(Intent(this, FirstScreen::class.java))
        finish()
    }

    private fun initializeCometChatAsync() {
        // Check CometChat status asynchronously without blocking UI
        if (MyApplication.isCometChatInitialized) {
            Log.d(TAG, "CometChat is ready")
            ensureCometChatLogin()
        } else {
            Log.d(TAG, "CometChat not ready, waiting for initialization...")

            // Wait for CometChat initialization in background
            cometChatManager.waitForInitialization { isInitialized ->
                runOnUiThread {
                    if (isInitialized) {
                        Log.d(TAG, "CometChat initialization completed")
                        ensureCometChatLogin()
                    } else {
                        Log.e(TAG, "CometChat initialization timeout")
                        // Try manual initialization as fallback
                        cometChatManager.initializeCometChat(this) { success, message ->
                            runOnUiThread {
                                if (success) {
                                    Log.d(TAG, "CometChat manual init successful: $message")
                                    ensureCometChatLogin()
                                } else {
                                    Log.e(TAG, "CometChat manual init failed: $message")
                                    // App still works without chat features
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensureCometChatLogin() {
        val userModel = preferenceManager.userModel
        if (userModel?.uid != null && !cometChatManager.isCometChatLoggedIn()) {
            Log.d(TAG, "CometChat not logged in, attempting login...")

            cometChatManager.loginToCometChat(userModel.uid!!) { success, user, message ->
                runOnUiThread {
                    if (success) {
                        Log.d(TAG, "CometChat login successful in MainScreen")
                    } else {
                        Log.w(TAG, "CometChat login failed: $message")
                        // Try to create user if login fails
                        cometChatManager.createCometChatUser(userModel) { created, createMsg ->
                            runOnUiThread {
                                if (created) {
                                    cometChatManager.loginToCometChat(userModel.uid!!) { retrySuccess, _, retryMessage ->
                                        runOnUiThread {
                                            if (retrySuccess) {
                                                Log.d(TAG, "CometChat login successful after user creation")
                                            } else {
                                                Log.e(TAG, "CometChat login failed again: $retryMessage")
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "User creation failed: $createMsg")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val currentUser: User? = cometChatManager.getCurrentCometChatUser()
            Log.d(TAG, "Already logged in as ${currentUser?.uid ?: "null"}")
        }
    }

    private fun setupMainScreen() {
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_screen)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupBackPressHandling()
        setupNotificationBell()
        observeNotifications()
        setupMenuButton()

        // Default fragment = Home
        replaceFragment(HomeFragment(), "Queries")

        with(binding) {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.Add -> {
                        startActivity(Intent(this@MainScreen, ChooseCommunityActivity::class.java))
                        true
                    }
                    R.id.community -> {
                        replaceFragment(CommunityFragment(), "Communities")
                        true
                    }
                    R.id.chat -> {
                        navigateToChatActivity()
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

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupNavigationDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        binding.navigationView.setNavigationItemSelectedListener(this)
        fixNavigationViewSpacing()
    }

    private fun fixNavigationViewSpacing() {
        try {
            binding.navigationView.setPadding(0, 0, 0, 0)
            val navigationMenuView = binding.navigationView.getChildAt(0) as? android.widget.ListView
            navigationMenuView?.apply {
                setPadding(0, 0, 0, 0)
                dividerHeight = 0
                requestLayout()
            }
            findRecyclerViewInNavigationView(binding.navigationView)?.apply {
                setPadding(0, 0, 0, 0)
                requestLayout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing navigation spacing: ${e.message}")
        }
    }

    private fun findRecyclerViewInNavigationView(view: View): androidx.recyclerview.widget.RecyclerView? {
        if (view is androidx.recyclerview.widget.RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findRecyclerViewInNavigationView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun setupNavigationHeader() {
        try {
            val headerView = binding.navigationView.getHeaderView(0)
            val ivProfileImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivProfileImage)
            val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)

            preferenceManager.userModel?.let { user ->
                tvUserName.text = user.fullName ?: "User Name"
                tvUserEmail.text = user.email ?: "user@example.com"

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
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation header: ${e.message}")
        }
    }

    private fun setupNotificationBell() {
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            motionToastUtil.showInfoToast(this, "Opening notifications...")
        }
    }

    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(HomeFragment(), "Queries")
                binding.bottomNavigationView.selectedItemId = R.id.home
            }
            R.id.nav_my_queries -> motionToastUtil.showInfoToast(this, "My Queries - Coming Soon!")
            R.id.nav_communities -> {
                replaceFragment(CommunityFragment(), "Communities")
                binding.bottomNavigationView.selectedItemId = R.id.community
            }
            R.id.nav_notifications -> startActivity(Intent(this, NotificationActivity::class.java))
            R.id.nav_profile -> motionToastUtil.showInfoToast(this, "Profile - Coming Soon!")
            R.id.nav_settings -> motionToastUtil.showInfoToast(this, "Settings - Coming Soon!")
            R.id.nav_about -> motionToastUtil.showInfoToast(this, "About - Coming Soon!")
            R.id.nav_logout -> showLogoutConfirmation()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        cometChatManager.logoutFromCometChat { success, message ->
            if (success) Log.d(TAG, "CometChat logout successful: $message")
            else Log.w(TAG, "CometChat logout failed: $message")

            preferenceManager.clearUserData()
            motionToastUtil.showSuccessToast(this, "Logged out successfully")

            val intent = Intent(this, FirstScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun observeNotifications() {
        val userId = preferenceManager.userId
        if (!userId.isNullOrEmpty()) {
            notificationViewModel.getUnreadNotificationCount(userId)
            notificationViewModel.unreadCount.observe(this) { state ->
                when (state) {
                    is UiState.Success -> updateNotificationBadge(state.data)
                    is UiState.Failure -> {
                        Log.e(TAG, "Failed to get unread count: ${state.error}")
                        updateNotificationBadge(0)
                    }
                    is UiState.Loading -> Log.d(TAG, "Loading notification count...")
                }
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        with(binding) {
            if (count > 0) {
                notificationBadge.visibility = View.VISIBLE
                notificationBadge.text = if (count > 99) "99+" else count.toString()
                notificationBadge.bringToFront()
            } else {
                notificationBadge.visibility = View.GONE
            }
        }
        binding.notificationContainer.invalidate()
        binding.notificationContainer.requestLayout()
    }

    fun updateToolbarTitle(title: String) {
        binding.toolbarTitle.text = title
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
        binding.toolbarTitle.text = title
    }

    override fun onResume() {
        super.onResume()

        // Only call functions that need binding if it's initialized
        if (::binding.isInitialized) {
            val userId = preferenceManager.userId
            if (!userId.isNullOrEmpty()) {
                notificationViewModel.getUnreadNotificationCount(userId)
            }
            setupNavigationHeader()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationViewModel.removeNotificationListener()
    }

    private fun navigateToChatActivity() {
        val userModel = preferenceManager.userModel
        val userId = preferenceManager.userId

        if (userModel?.uid == null || userId.isNullOrEmpty()) {
            motionToastUtil.showFailureToast(this, "Please login to access chat")
            return
        }

        // Check if CometChat is initialized first
        if (!MyApplication.isCometChatInitialized) {
            motionToastUtil.showInfoToast(this, "Initializing chat services...")

            // Wait for initialization
            cometChatManager.waitForInitialization { isInitialized ->
                runOnUiThread {
                    if (isInitialized) {
                        proceedWithChatLogin(userModel)
                    } else {
                        motionToastUtil.showFailureToast(this, "Chat services are not available. Please try again later.")
                    }
                }
            }
            return
        }

        // If initialized, proceed with login check
        proceedWithChatLogin(userModel)
    }

    private fun proceedWithChatLogin(userModel: UserModel) {
        Log.d(TAG, "Proceeding with chat login for user: ${userModel.uid}")

        // Double-check CometChat initialization
        if (!MyApplication.isCometChatInitialized) {
            motionToastUtil.showFailureToast(this, "Chat service not ready")
            return
        }

        if (cometChatManager.isCometChatLoggedIn()) {
            // Already logged in, show instruction
            Log.d(TAG, "User already logged in to CometChat")
            launchChatActivity()
        } else {
            // Need to login to CometChat
            Log.d(TAG, "User not logged in to CometChat, attempting login...")
            motionToastUtil.showInfoToast(this, "Connecting to chat...")

            cometChatManager.loginToCometChat(userModel.uid!!) { success, user, message ->
                runOnUiThread {
                    Log.d(TAG, "CometChat login result: success=$success, message=$message")

                    if (success && user != null) {
                        Log.d(TAG, "CometChat login successful, showing instruction")
                        launchChatActivity()
                    } else {
                        Log.w(TAG, "CometChat login failed, trying to create user first")
                        // Try to create user if login fails
                        motionToastUtil.showInfoToast(this, "Setting up chat account...")

                        cometChatManager.createCometChatUser(userModel) { created, createMessage ->
                            runOnUiThread {
                                if (created) {
                                    // Retry login after user creation
                                    cometChatManager.loginToCometChat(userModel.uid!!) { retrySuccess, retryUser, retryMessage ->
                                        runOnUiThread {
                                            if (retrySuccess && retryUser != null) {
                                                motionToastUtil.showSuccessToast(this, "Chat account ready!")
                                                launchChatActivity()
                                            } else {
                                                motionToastUtil.showFailureToast(
                                                    this,
                                                    "Failed to connect to chat: $retryMessage"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    motionToastUtil.showFailureToast(
                                        this,
                                        "Failed to setup chat account: $createMessage"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchChatActivity() {
        // Since ChatActivity requires a specific user, show a helpful message
        motionToastUtil.showInfoToast(
            this,
            "To start a chat, go to a query and click the 'Chat' button to message the query author"
        )
    }
}