package com.example.ask.utilities

import androidx.fragment.app.Fragment
import jakarta.inject.Inject


open class BaseFragment : Fragment(){
    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var motionToastUtil: MotionToastUtil
}