package com.example.ask.mainModule.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ask.addModule.repositories.RepositoryQuery
import com.example.ask.search.repositories.SearchRepository
import com.example.ask.utilities.PreferenceManager // ✅ Import PreferenceManager
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val queryRepository: RepositoryQuery,
    private val searchRepository: SearchRepository,
    private val preferenceManager: PreferenceManager // ✅ Inject PreferenceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _uiState = MutableStateFlow<UiState<List<Any>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Any>>> = _uiState

    private val userId: String? = preferenceManager.userId // ✅ Get userId

    init {
        observeDataChanges()
    }

    // --- Coroutine Usage ---
    // `viewModelScope.launch` starts a lifecycle-aware coroutine.
    // `flatMapLatest` is a powerful coroutine Flow operator. It cancels the previous
    // network/db call (search or getQueries) as soon as a new search query comes in,
    // ensuring we only process results for the *latest* query.
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeDataChanges() {
        if (userId.isNullOrBlank()) {
            Log.e("HomeViewModel", "User is not logged in. Cannot fetch queries.")
            _uiState.value = UiState.Failure("User not logged in.")
            return
        }

        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        // ✅ FIXED: Call the new Flow function with the userId
                        queryRepository.getQueriesFromUserCommunitiesFlow(userId)
                    } else {
                        // This was already correct
                        searchRepository.search(query)
                    }
                }
                .collect { state ->
                    _uiState.value = state as UiState<List<Any>>
                }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun retry() {
        // Trigger a refresh by re-emitting the current query value
        onQueryChanged(_searchQuery.value)
    }
}