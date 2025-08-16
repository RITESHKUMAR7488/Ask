package com.example.ask.communityModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommunityModels(
    var communityId : String?=null,
    var communityName: String?=null,
    var role:String?=null,
    var userId: String?=null,
    var communityCode: String?=null,
    var joinedAt: Long? = null
): Parcelable
