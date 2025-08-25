package com.example.ask.onBoardingModule.repositories

import android.content.Context
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.PreferenceManager
import com.example.ask.utilities.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Inject

class OnBoardingRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OnBoardingRepository {

    private lateinit var userId:String
    @Inject
    lateinit var preferenceManager: PreferenceManager


    override fun register(
        context: Context,
        email: String,
        password: String,
        userModel: UserModel,
        result: (UiState<String>) -> Unit
    ) {
        preferenceManager=PreferenceManager(context)
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    val user=auth.currentUser
                    if (user!=null){
                        val userId=user.uid
                        preferenceManager.userId=userId
                        userModel.uid=userId
                        preferenceManager.userModel=userModel

                        sendUserData(context,userModel){state ->
                            when(state){
                                is UiState.Success ->{
                                    result.invoke(UiState.Success("User registered successfully"))
                                }
                                is UiState.Failure->{
                                    result.invoke(UiState.Failure(state.error))
                                }
                                is UiState.Loading->{
                                    result.invoke(UiState.Loading)
                                }

                            }
                            
                        }
                    }else{
                        result.invoke(UiState.Failure("User is null After Registration"))
                    }
                }

            }.addOnFailureListener{
                result.invoke(UiState.Failure(it.localizedMessage?:"Registration"))
            }


    }

    override fun logIn(
        context: Context,
        email: String,
        password: String,
        result: (UiState<String>) -> Unit
    )  {
        preferenceManager = PreferenceManager(context)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userId = auth.currentUser!!.uid
                    preferenceManager.userId = userId

                    // ✅ fetch user profile from Firestore
                    firestore.collection(Constant.USERS)
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val userModel = document.toObject(UserModel::class.java)
                                preferenceManager.userModel = userModel
                                result.invoke(UiState.Success("Login Successful"))
                            } else {
                                result.invoke(UiState.Failure("User data not found"))
                            }
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(
                                UiState.Failure(
                                    exception.localizedMessage ?: "Failed to load user data"
                                )
                            )
                        }
                } else {
                    result.invoke(UiState.Failure("Authentication failed: ${task.exception?.message}"))
                }
            }
            .addOnFailureListener {
                result.invoke(UiState.Failure(it.localizedMessage ?: "Login failed"))
            }
    }



    override fun sendUserData(
        context: Context,
        userModel: UserModel,
        result: (UiState<String>) -> Unit
    ) {
        preferenceManager=PreferenceManager(context)
        userId=preferenceManager.userId.toString()
        val document=firestore.collection(Constant.USERS).document(userId)
        userModel.uid=userId
        document.set(userModel).addOnSuccessListener {
            result.invoke(UiState.Success("Registered Successfully"))
        }.addOnFailureListener{exception->
            result.invoke(UiState.Failure(exception.localizedMessage))
        }



    }
}