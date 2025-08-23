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

    // ✅ FIXED: Following OnBoardingViewModel pattern - using private _mutableLiveData and public LiveData
    private val _addCommunity = MutableLiveData<UiState<CommunityModels>>()
    val addCommunity: LiveData<UiState<CommunityModels>> = _addCommunity

    private val _joinCommunity = MutableLiveData<UiState<CommunityModels>>()
    val joinCommunity: LiveData<UiState<CommunityModels>> = _joinCommunity

    private val _userCommunities = MutableLiveData<UiState<List<CommunityModels>>>()
    val userCommunities: LiveData<UiState<List<CommunityModels>>> get() = _userCommunities

    // ✅ FIXED: Following OnBoardingViewModel method signature pattern
    fun addCommunity(userId: String, model: CommunityModels, role: String) {
        _addCommunity.value = UiState.Loading
        repository.addCommunity(userId, model, role) {
            _addCommunity.value = it
        }
    }

    fun joinCommunity(userId: String, communityCode: String) {
        _joinCommunity.value = UiState.Loading
        repository.joinCommunity(userId, communityCode) {
            _joinCommunity.value = it
        }
    }

    fun getUserCommunities(userId: String) {
        _userCommunities.value = UiState.Loading
        repository.getUserCommunity(userId) { result ->
            _userCommunities.value = result
        }
    }

    fun removeCommunityListener() {
        repository.removeCommunityListener()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeCommunityListener()
    }
}
