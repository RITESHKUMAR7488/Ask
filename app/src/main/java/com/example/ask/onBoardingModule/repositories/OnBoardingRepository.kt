package com.example.ask.onBoardingModule.repositories

import android.content.Context
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.utilities.UiState

interface OnBoardingRepository {
    fun register(context: Context,email:String,password:String,userModel: UserModel,result: (UiState<String>)->Unit)
    fun logIn(context: Context,email: String,password: String,result: (UiState<String>) -> Unit)
    fun sendUserData(context: Context,userModel: UserModel,result: (UiState<String>) -> Unit)

}