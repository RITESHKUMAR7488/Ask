package com.example.ask.addModule.interfaces

import com.example.ask.addModule.models.ImageUploadResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query


interface ImageUploadApi {
    @Multipart
    @POST("api/1/upload")
    fun uploadImage(
        @Query("key") apiKey: String,
        @Query("action") action: String = "upload",
        @Part image: MultipartBody.Part
    ): Call<ImageUploadResponse>
}
