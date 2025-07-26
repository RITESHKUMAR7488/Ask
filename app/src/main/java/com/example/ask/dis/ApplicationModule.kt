package com.example.ask.dis

import android.content.Context
import com.example.ask.MyApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Provides
    fun provideMyApp (@ApplicationContext context: Context):MyApplication{
        return context.applicationContext as MyApplication
    }
}