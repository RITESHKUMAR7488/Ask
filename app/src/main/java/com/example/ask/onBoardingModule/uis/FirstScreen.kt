package com.example.ask.onBoardingModule.uis

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.ask.R
import com.example.ask.databinding.ActivityFirstScreenBinding
import com.example.ask.utilities.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirstScreen : BaseActivity() {
    private lateinit var binding:ActivityFirstScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=DataBindingUtil.setContentView(this,R.layout.activity_first_screen)
        with(binding){
            btnSignIn.setOnClickListener{
                startActivity(Intent(this@FirstScreen,SignInActivity::class.java))
            }
            btnSignUp.setOnClickListener{
                startActivity(Intent(this@FirstScreen,RegisterActivity::class.java))
            }

        }

    }
}