package com.example.ask.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.example.ask.onBoardingModule.models.UserModel
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class PreferenceManager @Inject constructor(@ApplicationContext val context: Context) {
    private var mPreferences: SharedPreferences = context.getSharedPreferences(
        Constant.AUTH,
        AppCompatActivity.MODE_PRIVATE
    )

    private var editor: SharedPreferences.Editor = mPreferences.edit()

    var isGmailLoggedIn: Boolean
        get() = mPreferences.getBoolean(Constant.LOGGED_IN_EMAIL, false)
        set(loggedIn) {
            editor.putBoolean(Constant.LOGGED_IN, loggedIn)
            editor.commit()
        }

    var userId: String?
        get() = mPreferences.getString(Constant.AUTH_MODEL, "")
        set(userId) {
            editor.putString(Constant.AUTH_MODEL, userId)
            editor.commit()
        }

    var userModel: UserModel?
        get() {
            val json = mPreferences.getString(Constant.USER_MODEL, null)
            return if (json != null) {
                Gson().fromJson(json, UserModel::class.java) // Convert JSON to UserModel
            } else {
                null
            }
        }
        set(value) {
            val json = Gson().toJson(value) // Convert UserModel to JSON
            editor.putString(Constant.USER_MODEL, json).apply()
        }

    var isLoggedIn: Boolean
        get() = mPreferences.getBoolean(Constant.LOGGED_IN_EMAIL, false)
        set(emailLogin) {
            editor.putBoolean(Constant.LOGGED_IN_EMAIL, emailLogin)
            editor.commit() // Save changes immediately
        }

    // Method to clear all user data on logout
    fun clearUserData() {
        editor.clear()
        editor.commit()
    }

    // Method to check if user session is valid
    fun isValidSession(): Boolean {
        return isLoggedIn && userModel != null && !userId.isNullOrEmpty()
    }
}