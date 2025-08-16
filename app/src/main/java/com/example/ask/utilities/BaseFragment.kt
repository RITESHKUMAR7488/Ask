package com.example.ask.utilities

import android.app.Fragment
import jakarta.inject.Inject
import www.sanju.motiontoast.MotionToast


open class BaseFragment : Fragment(){
    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var motionToastUtil: MotionToastUtil
}