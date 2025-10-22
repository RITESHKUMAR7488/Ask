package com.example.ask.search.repositories

import com.example.ask.search.model.SearchResult
import com.example.ask.utilities.UiState
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    // Uses Flow for streaming results asynchronously
    fun search(query: String): Flow<UiState<List<SearchResult>>>
}