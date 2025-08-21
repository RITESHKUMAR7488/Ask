package com.example.ask.MainModule.uis.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.ask.MainModule.uis.fragments.HomeFragment
import com.example.ask.R
import com.example.ask.addModule.uis.AddQueryActivity
import com.example.ask.communityModule.uis.Activities.CreateCommunityActicity
import com.example.ask.communityModule.uis.fragments.CommunityFragment
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.withContext
@AndroidEntryPoint
class MainScreen : BaseActivity() {
    private lateinit var binding: ActivityMainScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_screen)

        // Default fragment = Home
        replaceFragment(HomeFragment(), "Queries")

        with(binding) {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.Add -> {
                        val intent = Intent(this@MainScreen, AddQueryActivity::class.java)
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
