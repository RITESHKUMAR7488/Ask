package com.example.ask.utilities

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
open class BaseActivity:AppCompatActivity() {


    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var motionToastUtil: MotionToastUtil
}