package com.example.ask.communityModule.repositories

import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.utilities.UiState

interface CommunityRepository {
    fun addCommunity(userId: String,model: CommunityModels,role:String,result: (UiState<CommunityModels>)-> Unit)
    fun getUserCommunity(userId: String,models: CommunityModels,role: String,result: (UiState<List<CommunityModels>>) -> Unit)
    fun removeCommunityListener()
}