package com.example.ask.onBoardingModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel(
    var uid: String? = null,
    var email: String? = null,
    var fullName: String? = null,
    var password: String? = null,
    var mobileNumber: String? = null,
    var imageUrl: String? = null,
    var address: String? = null,
    var profilePicUrl: String = ""
):Parcelable
