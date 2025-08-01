package com.example.ask.addModule.dis

import com.example.ask.addModule.interfaces.ImageUploadApi
import com.example.ask.addModule.repositories.RepositoryMain
import com.example.ask.addModule.repositories.RepositoryMainImpl
import com.example.ask.utilities.Constant
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Singleton
    @Provides
    @Named("ImageUploadRetrofit") // ✅ Naming the Retrofit instance for image upload
    fun provideRetroFit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL_IMAGE_UPLOAD)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    @Singleton
    @Provides
    fun provideImageUploadApi(@Named("ImageUploadRetrofit") retrofit: Retrofit): ImageUploadApi {
        return retrofit.create(ImageUploadApi::class.java)
    }
    @Provides
    @Singleton
    fun provideRepositoryMain(
        database: FirebaseFirestore,
        imageUploadApi: ImageUploadApi,
    ): RepositoryMain {
        return RepositoryMainImpl(database, imageUploadApi)
    }
}