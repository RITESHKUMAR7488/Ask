package com.example.ask.profileModule.repositories

import android.net.Uri
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.utilities.UiState
import java.io.File

interface ProfileRepository {
    suspend fun getUserProfile(userId: String): UiState<UserModel>
    suspend fun updateUserProfile(userId: String, updatedData: Map<String, Any>): UiState<Unit>
    suspend fun uploadProfileImage(imageFile: File, apiKey: String): UiState<ImageUploadResponse>
}