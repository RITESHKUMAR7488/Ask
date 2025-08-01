package com.example.ask.utilities

import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject

open class BaseActivity:AppCompatActivity() {


    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var motionToastUtil: MotionToastUtil
}