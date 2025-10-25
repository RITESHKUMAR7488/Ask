package com.example.ask.communityModule.viewModels // Or a more suitable package

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.repositories.RepositoryQuery // Reuse existing query repo
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityPostsViewModel @Inject constructor(
    private val queryRepository: RepositoryQuery
) : ViewModel() {

    private val _postsState = MutableLiveData<UiState<List<QueryModel>>>()
    val postsState: LiveData<UiState<List<QueryModel>>> = _postsState

    // --- Coroutine Usage ---
    // viewModelScope.launch starts a coroutine tied to this ViewModel's lifecycle.
    // The queryRepository.getCommunityPosts function (which we'll add)
    // will likely be a suspend function or return a Flow, performing database
    // operations asynchronously off the main thread.
    fun fetchPostsForCommunity(communityId: String) {
        _postsState.value = UiState.Loading
        viewModelScope.launch {
            // Assuming you add a method like getCommunityPosts in your RepositoryQuery
            // For now, let's simulate fetching logic or adapt getAllQueries if suitable
            queryRepository.getCommunityPosts(communityId) { result ->
                _postsState.postValue(result) // Use postValue if callback is on bg thread
            }
            // OR if you adapt getAllQueries:
            /*
            queryRepository.getAllQueries { result ->
                when(result) {
                    is UiState.Success -> {
                        val filtered = result.data.filter { it.communityId == communityId }
                        _postsState.postValue(UiState.Success(filtered))
                    }
                    is UiState.Failure -> _postsState.postValue(result)
                    is UiState.Loading -> _postsState.postValue(UiState.Loading) // Should already be loading
                }
            }
            */
        }
    }
}