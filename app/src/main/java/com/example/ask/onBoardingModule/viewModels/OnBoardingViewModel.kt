package com.example.ask.onBoardingModule.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.onBoardingModule.repositories.OnBoardingRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
@HiltViewModel

class OnBoardingViewModel @Inject constructor(private val repository: OnBoardingRepository):ViewModel() {


    private val _register=MutableLiveData<UiState<String>>()
    val register: LiveData<UiState<String>> = _register
    fun registerUser(context: Context, email: String, passWord:String, userModel: UserModel){
        _register.value= UiState.Loading
        repository.register(context,email,passWord,userModel){
            _register.value=it
        }
    }

    private val _login=MutableLiveData<UiState<String>>()
    val login:LiveData<UiState<String>> =_login

    fun login(context: Context,email: String,passWord: String){
        _login.value=UiState.Loading
        repository.logIn(context,email, passWord){
            Log.d("somethingReturn",it.toString())
            _login.value=it
        }
    }

}