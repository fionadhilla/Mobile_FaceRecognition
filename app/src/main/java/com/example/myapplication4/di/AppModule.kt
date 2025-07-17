package com.example.myapplication4.di

import android.content.Context
import com.example.myapplication4.data.database.AppDatabase
import com.example.myapplication4.data.database.dao.PendingSyncDao
import com.example.myapplication4.data.database.dao.UserDao
import com.example.myapplication4.data.repository.HistoryRepository
import com.example.myapplication4.data.repository.HistoryRepositoryImpl
import com.example.myapplication4.data.repository.LoginRepository
import com.example.myapplication4.data.repository.LoginRepositoryImpl
import com.example.myapplication4.data.repository.UserProfileRepository
import com.example.myapplication4.data.repository.UserProfileRepositoryImpl
import com.example.myapplication4.data.repository.UserRepository
import com.example.myapplication4.data.repository.UserRepositoryImpl
import com.example.myapplication4.domain.usecase.GetUserProfileUseCase
import com.example.myapplication4.domain.usecase.LoginUseCase
import com.example.myapplication4.domain.usecase.RegisterUserWithFaceUseCase
import com.example.myapplication4.domain.usecase.UpdateUserProfileUseCase
import com.example.myapplication4.face.FaceEmbedder
import com.example.myapplication4.face.FaceNetModel
import androidx.room.Room
import com.example.myapplication4.domain.usecase.VerifyFaceUseCase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideUserProfileRepository(): UserProfileRepository {
        return UserProfileRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetUserProfileUseCase(repository: UserProfileRepository): GetUserProfileUseCase {
        return GetUserProfileUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateUserProfileUseCase(repository: UserProfileRepository): UpdateUserProfileUseCase {
        return UpdateUserProfileUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "face_recognition_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePendingSyncDao(appDatabase: AppDatabase): PendingSyncDao {
        return appDatabase.pendingSyncDao()
    }

    @Provides
    @Singleton
    fun provideFaceRepository(
        userDao: UserDao,
        pendingSyncDao: PendingSyncDao
    ): UserRepository {
        return UserRepositoryImpl(userDao, pendingSyncDao)
    }

    @Provides
    @Singleton
    fun provideRegisterUserWithFaceUseCase(userRepository: UserRepository): RegisterUserWithFaceUseCase {
        return RegisterUserWithFaceUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideVerifyFaceUseCase(userRepository: UserRepository): VerifyFaceUseCase {
        return VerifyFaceUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideFaceNetModel(@ApplicationContext context: Context): FaceNetModel {
        return FaceNetModel(context)
    }

    @Provides
    @Singleton
    fun provideFaceEmbedder(faceNetModel: FaceNetModel): FaceEmbedder {
        return FaceEmbedder(faceNetModel)
    }
}