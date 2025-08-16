package com.example.ask.communityModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.communityModule.repositories.CommunityRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(private val repository: CommunityRepository): ViewModel() {

    fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String
    ): LiveData<UiState<CommunityModels>> {
        val successData= MutableLiveData<UiState<CommunityModels>>()
        successData.value= UiState.Loading
        repository.addCommunity(userId,model,role){
            successData.value= it
        }
        return successData
    }

    // 🔹 Get User Communities (Live updates)
    private val _userCommunities = MutableLiveData<UiState<List<CommunityModels>>>()
    val userCommunities: LiveData<UiState<List<CommunityModels>>> get() = _userCommunities

    fun getUserCommunities(userId: String) {
        _userCommunities.value = UiState.Loading
        repository.getUserCommunity(userId) {
            _userCommunities.value = it
        }
    }

    // 🔹 Stop listening when screen closes
    fun removeCommunityListener() {
        repository.removeCommunityListener()
    }
}