package com.example.ask.mainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment


import com.example.ask.R
import com.example.ask.addModule.uis.ChooseCommunityActivity
import com.example.ask.communityModule.uis.fragments.CommunityFragment
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.mainModule.uis.fragments.HomeFragment
import com.example.ask.onBoardingModule.uis.FirstScreen
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainScreen : BaseActivity() {
    private lateinit var binding: ActivityMainScreenBinding

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
}