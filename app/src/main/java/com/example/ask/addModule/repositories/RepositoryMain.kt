package com.example.ask.addModule.repositories

import androidx.lifecycle.MutableLiveData
import com.example.ask.addModule.models.ImageUploadResponse
import java.io.File

interface RepositoryMain {
    fun uploadImage(
        imageFile: File,
        apiKey: String,
        data: MutableLiveData<ImageUploadResponse>,
        error: MutableLiveData<Throwable>
    )
}