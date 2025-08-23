package com.example.ask.onBoardingModule.uis

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.ask.MainModule.uis.activities.MainScreen
import com.example.ask.R
import com.example.ask.databinding.ActivityRegisterBinding
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.onBoardingModule.repositories.OnBoardingRepository
import com.example.ask.onBoardingModule.viewModels.OnBoardingViewModel
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.MotionToastUtil
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val onBoardingViewModel: OnBoardingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        with(binding) {
            btnSignUp.setOnClickListener{
                validate()
            }

        }

    }

    private fun validate() {
        val fullName = binding.etFullName.text.toString().trim()
        val mobileNumber = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (fullName.isBlank()) {
            binding.etFullName.error = "Please Enter Name"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid Email"
        } else if (password.length < 4) {
            binding.etPassword.error = "Password must be at least 8 character"
        } else if (mobileNumber.length < 10) {
            binding.etPhone.error = "Enter Valid Number"
        } else {
            val model = UserModel()
            model.fullName = fullName
            model.email = email
            model.password = password
            model.mobileNumber = mobileNumber

            onBoardingViewModel.registerUser(this, email, password, model)
            onBoardingViewModel.register.observe(this) {
                when (it) {
                    is UiState.Failure -> {
                        motionToastUtil.showFailureToast(
                            this, it.error, duration = MotionToast.SHORT_DURATION
                        )
                        binding.progressBar.visibility = View.GONE
                        binding.textView3.visibility = View.VISIBLE
                    }

                    UiState.Loading -> {
                        binding.progressBar.visibility = View.GONE
                        binding.textView3.visibility = View.VISIBLE

                    }
                    is UiState.Success<*> ->{
                        binding.progressBar.visibility = View.GONE
                        binding.textView3.visibility = View.VISIBLE
                        preferenceManager.isLoggedIn=true

                        val userModel=UserModel(
                            uid=preferenceManager.userId,
                            email=email,
                            fullName=fullName,
                            mobileNumber=mobileNumber,
                            password = password
                        )
                        preferenceManager.userModel=userModel
                        motionToastUtil.showSuccessToast(
                            this,
                            "Registration Successfully",
                            duration = MotionToast.SHORT_DURATION


                        )
                        startActivity(Intent(this, MainScreen::class.java))

                    }
                }
            }

        }
    }
}