package com.artsiom.footballpulse.di

import com.artsiom.footballpulse.data.FootballRepositoryImpl
import com.artsiom.footballpulse.data.remote.FootballApiService
import com.artsiom.footballpulse.domain.repository.FootballRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideFootballRepository(api: FootballApiService): FootballRepository = FootballRepositoryImpl(api)
}
