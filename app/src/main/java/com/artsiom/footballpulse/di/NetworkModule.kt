package com.artsiom.footballpulse.di

import com.artsiom.footballpulse.data.remote.FootballApiService
import com.artsiom.footballpulse.data.remote.RetrofitInstance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideFootballApiService(): FootballApiService = RetrofitInstance().api
}
