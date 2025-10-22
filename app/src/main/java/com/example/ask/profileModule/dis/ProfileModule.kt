package com.example.ask.profileModule.dis

import com.example.ask.addModule.interfaces.ImageUploadApi // Reusing the existing API interface
import com.example.ask.profileModule.repositories.ProfileRepository
import com.example.ask.profileModule.repositories.ProfileRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun provideProfileRepository(
        firestore: FirebaseFirestore,
        imageUploadApi: ImageUploadApi // Injecting the existing API
    ): ProfileRepository {
        return ProfileRepositoryImpl(firestore, imageUploadApi)
    }
}