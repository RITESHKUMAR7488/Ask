package com.example.ask.addModule.repositories

import androidx.lifecycle.MutableLiveData
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.addModule.models.QueryModel
import com.example.ask.utilities.UiState
import kotlinx.coroutines.flow.Flow
import java.io.File

interface RepositoryQuery {
    fun uploadImage(
        imageFile: File,
        apiKey: String,
        data: MutableLiveData<ImageUploadResponse>,
        error: MutableLiveData<Throwable>
    )

    fun addQuery(queryModel: QueryModel, result: (UiState<String>) -> Unit)

    fun getUserQueries(userId: String, result: (UiState<List<QueryModel>>) -> Unit)

    fun getAllQueries(result: (UiState<List<QueryModel>>) -> Unit)

    // ✅ NEW: Get queries from user's joined communities only
    fun getQueriesFromUserCommunities(userId: String, result: (UiState<List<QueryModel>>) -> Unit)

    fun updateQueryStatus(queryId: String, status: String, result: (UiState<String>) -> Unit)
    fun deleteQuery(queryId: String, result: (UiState<String>) -> Unit)
    fun getQueriesFromUserCommunitiesFlow(userId: String): Flow<UiState<List<QueryModel>>>
    fun getCommunityPosts(communityId: String, result: (UiState<List<QueryModel>>) -> Unit) // Add this


}