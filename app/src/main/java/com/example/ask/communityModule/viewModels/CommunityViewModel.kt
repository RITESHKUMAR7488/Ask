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
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository
) : ViewModel() {

    // 🔹 Add Community
    fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String
    ): LiveData<UiState<CommunityModels>> {
        val successData = MutableLiveData<UiState<CommunityModels>>()
        successData.value = UiState.Loading
        repository.addCommunity(userId, model, role) { result ->
            successData.value = result
        }
        return successData
    }

    // 🔹 Get User Communities (Live updates)
    private val _userCommunities = MutableLiveData<UiState<List<CommunityModels>>>()
    val userCommunities: LiveData<UiState<List<CommunityModels>>> get() = _userCommunities

    fun getUserCommunities(userId: String) {
        _userCommunities.value = UiState.Loading

        // Create dummy CommunityModels and role for the repository call
        val dummyModel = CommunityModels()
        val dummyRole = ""

        repository.getUserCommunity(userId, dummyModel, dummyRole) { result ->
            _userCommunities.value = result
        }
    }

    // 🔹 Stop listening when screen closes
    fun removeCommunityListener() {
        repository.removeCommunityListener()
    }

    // 🔹 Clean up resources when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        removeCommunityListener()
    }
}