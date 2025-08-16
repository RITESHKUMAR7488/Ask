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
import com.example.ask.databinding.ActivityMainScreenBinding
import com.example.ask.utilities.BaseActivity
import kotlinx.coroutines.withContext

class MainScreen : BaseActivity() {
    private lateinit var binding: ActivityMainScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=DataBindingUtil.setContentView(this,R.layout.activity_main_screen)
        with(binding){
            bottomNavigationView.setOnNavigationItemSelectedListener { item->
                when(item.itemId){
                    R.id.Add->{
                        val intent= Intent(this@MainScreen,AddQueryActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.community->{
                        val intent= Intent(this@MainScreen, CreateCommunityActicity::class.java)
                        startActivity(intent)
                        true
                    }

                    else -> {
                        replaceFragment(HomeFragment())
                        false


                    }
                }
            }
        }

    }
    private fun replaceFragment(fragment: Fragment) {

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

    }
}