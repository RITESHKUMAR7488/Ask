package com.example.ask.addModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.repositories.RepositoryMain
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: RepositoryMain
) : ViewModel() {

    private val _imageUpload = MutableLiveData<ImageUploadResponse>()
    val imageUpload: LiveData<ImageUploadResponse> = _imageUpload

    private val _imageUploadError = MutableLiveData<Throwable>()
    val imageUploadError: LiveData<Throwable> = _imageUploadError

    private val _addQuery = MutableLiveData<UiState<String>>()
    val addQuery: LiveData<UiState<String>> = _addQuery

    private val _userQueries = MutableLiveData<UiState<List<QueryModel>>>()
    val userQueries: LiveData<UiState<List<QueryModel>>> = _userQueries

    private val _allQueries = MutableLiveData<UiState<List<QueryModel>>>()
    val allQueries: LiveData<UiState<List<QueryModel>>> = _allQueries

    private val _updateStatus = MutableLiveData<UiState<String>>()
    val updateStatus: LiveData<UiState<String>> = _updateStatus

    fun uploadImage(imageFile: File, apiKey: String) {
        repository.uploadImage(imageFile, apiKey, _imageUpload, _imageUploadError)
    }

    fun addQuery(queryModel: QueryModel) {
        _addQuery.value = UiState.Loading
        repository.addQuery(queryModel) {
            _addQuery.value = it
        }
    }

    fun getUserQueries(userId: String) {
        _userQueries.value = UiState.Loading
        repository.getUserQueries(userId) {
            _userQueries.value = it
        }
    }

    fun getAllQueries() {
        _allQueries.value = UiState.Loading
        repository.getAllQueries {
            _allQueries.value = it
        }
    }

    fun updateQueryStatus(queryId: String, status: String) {
        _updateStatus.value = UiState.Loading
        repository.updateQueryStatus(queryId, status) {
            _updateStatus.value = it
        }
    }
}