package com.example.ask.onBoardingModule.uis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.ask.mainModule.uis.activities.MainScreen
import com.example.ask.R
import com.example.ask.databinding.ActivitySignInBinding
import com.example.ask.onBoardingModule.viewModels.OnBoardingViewModel
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

@AndroidEntryPoint
class SignInActivity : BaseActivity() {
    private lateinit var binding: ActivitySignInBinding
    private val onBoardingViewModel: OnBoardingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        with(binding) {
            btnSignIn.setOnClickListener{
                logIn()
            }
            btnSignUp.setOnClickListener {
                val intent= Intent(this@SignInActivity,RegisterActivity::class.java)
                startActivity(intent)
            }
            binding.tvForgotPassword.setOnClickListener {
                showForgotPasswordDialog()
            }


        }

    }

    private fun logIn() {
        with(binding) {
            when {
                etEmail.text.toString().isBlank()->{
                    etEmail.error="please enter email"

                }
                etPassword.text.toString().isBlank()->{
                    etPassword.error="please enter the password"
                }
                else ->{
                    val email=etEmail.text.toString().trim()
                    val password=etPassword.text.toString().trim()
                    onBoardingViewModel.login(this@SignInActivity,email,password)
                    onBoardingViewModel.login.observe(this@SignInActivity){state ->
                        when(state){
                            is UiState.Loading ->{
                                tvSignIn.visibility= View.GONE
                                progressBar.visibility=View.VISIBLE
                            }
                            is UiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvSignIn.visibility = View.VISIBLE
                                preferenceManager.isLoggedIn = true  // ✅ keep this after repository sets userModel
                                startActivity(Intent(this@SignInActivity, MainScreen::class.java))
                                finish()
                            }
                            is UiState.Failure -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvSignIn.visibility = View.VISIBLE
                                motionToastUtil.showFailureToast(this@SignInActivity,state.error, duration = MotionToast.SHORT_DURATION)
                                Log.e("Login", "Login failed: ${state.error}")
                            }
                        }

                    }

                }


            }

        }

    }
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        builder.setView(view)
        builder.setPositiveButton("Reset") { _, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                onBoardingViewModel.forgotPassword(email)
            } else {
                motionToastUtil.showWarningToast(this, "Please enter your email.")
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}