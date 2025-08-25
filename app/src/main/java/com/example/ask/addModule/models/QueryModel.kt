package com.example.ask.addModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QueryModel(
    var queryId: String? = null,
    var userId: String? = null,
    var userName: String? = null,
    var userProfileImage: String? = null,
    var title: String? = null,
    var description: String? = null,
    var category: String? = null,
    var location: String? = null,
    var imageUrl: String? = null,
    var status: String? = "OPEN", // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    var timestamp: Long? = null,
    var responseCount: Int = 0,
    var tags: List<String>? = null,
    var priority: String? = "NORMAL", // HIGH, NORMAL, LOW
    // ✅ NEW: Added community fields
    var communityId: String? = null,
    var communityName: String? = null
) : Parcelable