package com.example.ask.utilities

import jakarta.inject.Inject
import www.sanju.motiontoast.MotionToast


open class BaseFragment {
    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var motionToastUtil: MotionToastUtil
}