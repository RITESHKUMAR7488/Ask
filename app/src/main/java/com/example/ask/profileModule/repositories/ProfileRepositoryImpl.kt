package com.example.ask.profileModule.repositories

import android.util.Log
import com.example.ask.addModule.interfaces.ImageUploadApi
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.awaitResponse
import java.io.File
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val imageUploadApi: ImageUploadApi
) : ProfileRepository {

    companion object {
        private const val TAG = "ProfileRepositoryImpl"
    }

    // --- Coroutine Usage ---
    // The 'suspend' keyword marks these functions as suspend functions,
    // allowing them to perform long-running operations (like network calls or DB access)
    // without blocking the main thread.
    // 'withContext(Dispatchers.IO)' is used to switch the execution context to an
    // I/O-optimized thread pool, which is ideal for network and disk operations.
    // This makes the code non-blocking and improves UI responsiveness.

    override suspend fun getUserProfile(userId: String): UiState<UserModel> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching profile for user: $userId")
            val documentSnapshot = firestore.collection(Constant.USERS)
                .document(userId)
                .get()
                .await() // .await() suspends the coroutine until the task completes

            val userModel = documentSnapshot.toObject(UserModel::class.java)
            if (userModel != null) {
                Log.d(TAG, "Profile fetched successfully for user: $userId")
                UiState.Success(userModel)
            } else {
                Log.w(TAG, "User document not found for user: $userId")
                UiState.Failure("User profile not found.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile for $userId", e)
            UiState.Failure(e.localizedMessage ?: "Failed to fetch profile.")
        }
    }

    override suspend fun updateUserProfile(userId: String, updatedData: Map<String, Any>): UiState<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating profile for user: $userId with data: $updatedData")
            firestore.collection(Constant.USERS)
                .document(userId)
                .update(updatedData)
                .await() // .await() suspends the coroutine until the update completes
            Log.d(TAG, "Profile updated successfully for user: $userId")
            UiState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile for $userId", e)
            UiState.Failure(e.localizedMessage ?: "Failed to update profile.")
        }
    }

    override suspend fun uploadProfileImage(imageFile: File, apiKey: String): UiState<ImageUploadResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Uploading profile image: ${imageFile.name}")
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("source", imageFile.name, requestFile)

            val response = imageUploadApi.uploadImage(apiKey, "upload", body).awaitResponse()

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                Log.d(TAG, "Image uploaded successfully. URL: ${responseBody.image?.displayUrl}")
                UiState.Success(responseBody)
            } else {
                val errorMsg = "Image upload failed: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                UiState.Failure(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile image", e)
            UiState.Failure(e.localizedMessage ?: "Network error during image upload.")
        } finally {
            // Clean up the temporary file if it exists and is no longer needed
            if (imageFile.exists() && imageFile.name.startsWith("temp_profile_")) {
                imageFile.delete()
                Log.d(TAG, "Deleted temporary image file: ${imageFile.name}")
            }
        }
    }
}