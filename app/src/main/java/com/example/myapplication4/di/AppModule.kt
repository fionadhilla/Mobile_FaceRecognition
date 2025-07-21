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
import com.example.myapplication4.domain.usecase.GetUserProfileUseCase
import com.example.myapplication4.domain.usecase.LoginUseCase
import com.example.myapplication4.domain.usecase.RegisterUserWithFaceUseCase
import com.example.myapplication4.domain.usecase.UpdateUserProfileUseCase
import com.example.myapplication4.face.FaceEmbedder
import com.example.myapplication4.face.FaceNetModel
import androidx.room.Room
import com.example.myapplication4.domain.usecase.VerifyFaceUseCase
import com.example.myapplication4.domain.usecase.SyncOfflineFacesUseCase
import okhttp3.OkHttpClient
import com.google.gson.Gson
import com.example.myapplication4.data.api.WebSocketClient
import com.example.myapplication4.data.repository.FaceRepository
import com.example.myapplication4.data.repository.FaceRepositoryImpl
import com.example.myapplication4.domain.utils.NetworkUtils

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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "faceRecogntionDB"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient, gson: Gson): WebSocketClient {
        val websocketClient = WebSocketClient(okHttpClient, gson)
        websocketClient.connect("ws://192.168.100.47:3000")
        return websocketClient
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
        pendingSyncDao: PendingSyncDao,
        webSocketClient: WebSocketClient,
        networkUtils: NetworkUtils
    ): FaceRepository {
        return FaceRepositoryImpl(userDao, pendingSyncDao, webSocketClient, networkUtils)
    }

    @Provides
    @Singleton
    fun provideRegisterFaceUseCase(faceRepository: FaceRepository): RegisterUserWithFaceUseCase {
        return RegisterUserWithFaceUseCase(faceRepository)
    }

    @Provides
    @Singleton
    fun provideVerifyFaceUseCase(faceRepository: FaceRepository): VerifyFaceUseCase {
        return VerifyFaceUseCase(faceRepository)
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

    @Provides
    @Singleton
    fun provideSyncOfflineFacesUseCase(
        faceRepository: FaceRepository,
        webSocketClient: WebSocketClient,
        networkUtils: NetworkUtils
    ): SyncOfflineFacesUseCase {
        return SyncOfflineFacesUseCase(faceRepository, webSocketClient, networkUtils)
    }
}