package com.example.myapplication4.di

import com.example.myapplication4.data.repository.HistoryRepository
import com.example.myapplication4.data.repository.HistoryRepositoryImpl
import com.example.myapplication4.data.repository.LoginRepository
import com.example.myapplication4.data.repository.LoginRepositoryImpl
import com.example.myapplication4.domain.usecase.LoginUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLoginRepository(): LoginRepository {
        return LoginRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(loginRepository: LoginRepository): LoginUseCase {
        return LoginUseCase(loginRepository)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(): HistoryRepository = HistoryRepositoryImpl()
}