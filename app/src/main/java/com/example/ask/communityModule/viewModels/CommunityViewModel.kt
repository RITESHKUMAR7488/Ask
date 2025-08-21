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
        val resultLiveData = MutableLiveData<UiState<CommunityModels>>()
        resultLiveData.value = UiState.Loading
        repository.addCommunity(userId, model, role) { result ->
            resultLiveData.value = result
        }
        return resultLiveData
    }

    // 🔹 Join Community
    fun joinCommunity(
        userId: String,
        communityCode: String
    ): LiveData<UiState<CommunityModels>> {
        val resultLiveData = MutableLiveData<UiState<CommunityModels>>()
        resultLiveData.value = UiState.Loading
        repository.joinCommunity(userId, communityCode) { result ->
            resultLiveData.value = result
        }
        return resultLiveData
    }

    // 🔹 Get User Communities
    private val _userCommunities = MutableLiveData<UiState<List<CommunityModels>>>()
    val userCommunities: LiveData<UiState<List<CommunityModels>>> get() = _userCommunities

    fun getUserCommunities(userId: String) {
        _userCommunities.value = UiState.Loading
        repository.getUserCommunity(userId) { result ->
            _userCommunities.value = result
        }
    }

    // 🔹 Explicit cleanup call for Fragment
    fun removeCommunityListener() {
        repository.removeCommunityListener()
    }

    // 🔹 Auto-cleanup when ViewModel dies
    override fun onCleared() {
        super.onCleared()
        repository.removeCommunityListener()
    }
}
