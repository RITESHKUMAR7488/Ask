package com.example.ask.communityModule.repositories

import androidx.lifecycle.LiveData
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.utilities.UiState

interface CommunityRepository {
    fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String,
        result: (UiState<CommunityModels>) -> Unit
    )

    fun getUserCommunity(
        userId: String,
        result: (UiState<List<CommunityModels>>) -> Unit
    )

    fun joinCommunity(
        userId: String,
        communityCode: String,
        result: (UiState<CommunityModels>) -> Unit
    )

    fun removeCommunityListener()
}
