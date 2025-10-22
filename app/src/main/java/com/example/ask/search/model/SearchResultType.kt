package com.example.ask.search.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Enum to define the type of search result
enum class SearchResultType {
    QUERY, COMMUNITY, USER
}

@Parcelize
data class SearchResult(
    val id: String,                 // Unique ID (QueryId, CommunityId, UserId)
    val type: SearchResultType,     // Type of result
    val title: String,              // Primary display text (Query title, Community name, User name)
    val subtitle: String?,          // Secondary display text (Query snippet, Community role, User email)
    val imageUrl: String?,          // Image URL (Query image, User profile pic, default for community)
    val timestamp: Long? = null     // Optional timestamp (e.g., for queries)
) : Parcelable