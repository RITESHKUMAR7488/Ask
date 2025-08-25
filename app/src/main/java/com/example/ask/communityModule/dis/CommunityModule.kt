package com.example.ask.communityModule.dis

import com.example.ask.communityModule.repositories.CommunityRepository
import com.example.ask.communityModule.repositories.CommunityRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CommunityModule {

    @Provides
    @Singleton
    fun provideCommunityRepository(
        firestore: FirebaseFirestore
    ): CommunityRepository {
        return CommunityRepositoryImpl(firestore)
    }
}
