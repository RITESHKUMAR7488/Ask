package com.example.ask.onBoardingModule.uis

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ask.MainModule.uis.activities.MainScreen
import com.example.ask.R
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashScreenActivity : BaseActivity() {

    companion object {
        private const val SPLASH_DELAY = 1500L // 1.5 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the splash screen layout
        setContentView(R.layout.activity_splash_screen)

        // Check login status after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToAppropriateScreen()
        }, SPLASH_DELAY)
    }

    private fun navigateToAppropriateScreen() {
        val intent = if (isUserLoggedIn()) {
            // User is logged in, go directly to MainScreen
            Intent(this, MainScreen::class.java)
        } else {
            // User is not logged in, go to FirstScreen for auth
            Intent(this, FirstScreen::class.java)
        }

        startActivity(intent)
        finish() // Remove splash from back stack
    }

    private fun isUserLoggedIn(): Boolean {
        return preferenceManager.isLoggedIn &&
                preferenceManager.userModel != null &&
                !preferenceManager.userId.isNullOrEmpty()
    }
}