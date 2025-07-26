package com.example.ask.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class PreferenceManager @Inject constructor(@ApplicationContext val context: Context) {
    private var mPreferences:SharedPreferences=context.getSharedPreferences(
        Constant.AUTH,
        AppCompatActivity.MODE_PRIVATE
    )

    private var editor:SharedPreferences.Editor=mPreferences.edit()

    var isGmailLoggedIn:Boolean
        get() =mPreferences.getBoolean(Constant.LOGGED_IN_EMAIL,false)
        set(loggedIn){
            editor.putBoolean(Constant.LOGGED_IN,loggedIn)
            editor.commit()
        }


}