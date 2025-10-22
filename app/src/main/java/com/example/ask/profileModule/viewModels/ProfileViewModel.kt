package com.example.ask.profileModule.viewModels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.profileModule.repositories.ProfileRepository
import com.example.ask.utilities.Constant
import com.example.ask.utilities.PreferenceManager
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    @ApplicationContext private val context: Context // Inject context for file operations
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _userProfileState = MutableStateFlow<UiState<UserModel>>(UiState.Loading)
    val userProfileState: StateFlow<UiState<UserModel>> = _userProfileState

    private val _updateProfileState = MutableStateFlow<UiState<Unit>?>(null) // Nullable initial state
    val updateProfileState: StateFlow<UiState<Unit>?> = _updateProfileState

    private val _uploadImageState = MutableStateFlow<UiState<String>?>(null) // State for image URL
    val uploadImageState: StateFlow<UiState<String>?> = _uploadImageState

    init {
        loadUserProfile()
    }

    // --- Coroutine Usage ---
    // 'viewModelScope.launch' starts a new coroutine tied to the ViewModel's lifecycle.
    // It automatically cancels when the ViewModel is cleared, preventing memory leaks.
    // We use it here to call the suspend functions from the repository.

    fun loadUserProfile() {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            _userProfileState.value = UiState.Failure("User not logged in.")
            return
        }
        _userProfileState.value = UiState.Loading
        viewModelScope.launch { // Coroutine launched in ViewModel scope
            _userProfileState.value = repository.getUserProfile(userId)
            // Update preference manager if fetch is successful
            if (_userProfileState.value is UiState.Success) {
                preferenceManager.userModel = (_userProfileState.value as UiState.Success<UserModel>).data
            }
        }
    }

    fun updateUserProfile(fullName: String, mobileNumber: String, address: String) {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            _updateProfileState.value = UiState.Failure("User not logged in.")
            return
        }

        val updatedData = mutableMapOf<String, Any>()
        // Only add fields that have changed and are not blank
        val currentUser = (userProfileState.value as? UiState.Success)?.data
        if (currentUser?.fullName != fullName && fullName.isNotBlank()) {
            updatedData["fullName"] = fullName
        }
        if (currentUser?.mobileNumber != mobileNumber && mobileNumber.isNotBlank()) {
            updatedData["mobileNumber"] = mobileNumber
        }
        if (currentUser?.address != address && address.isNotBlank()) {
            updatedData["address"] = address
        }


        if (updatedData.isEmpty()) {
            _updateProfileState.value = UiState.Success(Unit) // Indicate success if nothing changed
            Log.d(TAG,"No profile data changed.")
            _updateProfileState.value = null // Reset state after a short delay or navigation
            return
        }


        _updateProfileState.value = UiState.Loading
        viewModelScope.launch { // Coroutine for update
            val result = repository.updateUserProfile(userId, updatedData)
            _updateProfileState.value = result
            if (result is UiState.Success) {
                // Refresh local profile data after successful update
                loadUserProfile()
            }
            // Consider resetting the state after showing feedback in the UI
            // _updateProfileState.value = null
        }
    }

    fun uploadAndProfileImage(imageUri: Uri) {
        val userId = preferenceManager.userId
        if (userId.isNullOrBlank()) {
            _uploadImageState.value = UiState.Failure("User not logged in.")
            return
        }

        _uploadImageState.value = UiState.Loading
        viewModelScope.launch { // Coroutine for file processing and upload
            val imageFile = uriToFile(imageUri) // This involves I/O, keep it off the main thread
            if (imageFile == null) {
                _uploadImageState.value = UiState.Failure("Failed to process image file.")
                return@launch
            }

            val uploadResult = repository.uploadProfileImage(imageFile, Constant.IMAGE_API_KEY)

            when (uploadResult) {
                is UiState.Success -> {
                    val imageUrl = uploadResult.data.image?.displayUrl
                    if (imageUrl != null) {
                        // Update Firestore with the new image URL
                        val updateImageResult = repository.updateUserProfile(userId, mapOf("imageUrl" to imageUrl))
                        if (updateImageResult is UiState.Success) {
                            _uploadImageState.value = UiState.Success(imageUrl)
                            // Refresh local profile data
                            loadUserProfile()
                        } else {
                            _uploadImageState.value = UiState.Failure("Image uploaded, but failed to update profile URL.")
                        }
                    } else {
                        _uploadImageState.value = UiState.Failure("Image uploaded, but URL was missing in response.")
                    }
                }
                is UiState.Failure -> {
                    _uploadImageState.value = UiState.Failure(uploadResult.error)
                }
                UiState.Loading -> {
                    // Should not happen here, handled by initial state
                }
            }
            // Reset state after handling in UI
            // _uploadImageState.value = null
        }
    }

    // Helper function to convert Uri to File (runs on Dispatchers.IO)
    private suspend fun uriToFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            // Use a unique temporary file name
            val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                Log.d(TAG, "Successfully created temporary file: ${tempFile.absolutePath}")
                tempFile
            } else {
                Log.e(TAG, "Failed to create or write to temporary file.")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting Uri to File", e)
            null
        }
    }

    // Function to reset the update state, call this from Fragment/Activity after showing toast
    fun resetUpdateState() {
        _updateProfileState.value = null
    }

    // Function to reset the upload state
    fun resetUploadState() {
        _uploadImageState.value = null
    }
}