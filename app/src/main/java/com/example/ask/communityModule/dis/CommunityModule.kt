package com.example.ask.communityModule.dis

import com.example.ask.communityModule.repositories.CommunityRepository
import com.example.ask.communityModule.repositories.CommunityRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton


@Module // I'm a recipe book
@InstallIn(SingletonComponent::class) // For the whole app
class CommunityModule {

    @Provides // Here's how to cook this
    @Singleton // Only cook it once
    fun provideCommunityRepository(
        firestore: FirebaseFirestore // Ingredient
    ): CommunityRepository { // The dish name
        return CommunityRepositoryImpl(firestore) // Real dish
    }
}