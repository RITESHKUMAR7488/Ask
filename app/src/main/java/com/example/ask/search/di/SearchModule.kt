package com.example.ask.search.di

import com.example.ask.search.repositories.SearchRepository
import com.example.ask.search.repositories.SearchRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideSearchRepository(firestore: FirebaseFirestore): SearchRepository {
        return SearchRepositoryImpl(firestore)
    }
}