package com.example.myapplication4.di

import android.content.Context
import com.example.myapplication4.data.api.WebSocketAuthService
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
import com.example.myapplication4.data.database.AppDatabase // Tambahkan ini
import com.example.myapplication4.data.database.dao.PendingSyncDao // Tambahkan ini
import com.example.myapplication4.data.database.dao.UserDao // Tambahkan ini
import com.example.myapplication4.domain.usecase.VerifyFaceUseCase
import com.example.myapplication4.domain.usecase.SyncOfflineFacesUseCase
import okhttp3.OkHttpClient
import com.google.gson.Gson
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

import com.example.myapplication4.data.api.WebSocketClient
import com.example.myapplication4.data.repository.FaceRepository
import com.example.myapplication4.data.repository.FaceRepositoryImpl
import com.example.myapplication4.domain.utils.NetworkUtils
import com.example.myapplication4.modelLoad.AddFaceDetector
import com.example.myapplication4.ui.login.LoginStateViewModel
import com.example.myapplication4.modelLoad.YoloV8PeopleDetector
import com.example.myapplication4.modelLoad.YoloV6ActivityDetector
import com.example.myapplication4.modelLoad.GestureDetector
import com.example.myapplication4.modelLoad.EmotionDetector
import com.example.myapplication4.modelLoad.ThreatDetector

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
    fun provideWebSocketAuthService(): WebSocketAuthService {
        return WebSocketAuthService()
    }

    @Provides
    @Singleton
    fun provideLoginRepository(webSocketAuthService: WebSocketAuthService): LoginRepository {
        return LoginRepositoryImpl(webSocketAuthService)
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(loginRepository: LoginRepository): LoginUseCase {
        return LoginUseCase(loginRepository)
    }

    @Provides
    @Singleton
    fun provideLoginStateViewModel(): LoginStateViewModel {
        return LoginStateViewModel()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(): HistoryRepository = HistoryRepositoryImpl()

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        // Tambahkan parameter webSocketAuthService di sini
        webSocketAuthService: WebSocketAuthService
    ): UserProfileRepository {
        // Sekarang, teruskan webSocketAuthService ke konstruktor UserProfileRepositoryImpl
        return UserProfileRepositoryImpl(webSocketAuthService)
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
        websocketClient.connect("ws://192.168.32.47:3000")
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
    fun provideAddFaceDetector(@ApplicationContext context: Context): AddFaceDetector {
        return AddFaceDetector(context)
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

    @Provides
    @Singleton
    fun provideYoloV8PeopleDetector(@ApplicationContext context: Context): YoloV8PeopleDetector {
        return YoloV8PeopleDetector(context)
    }

    @Provides
    @Singleton
    fun provideYoloV6ActivityDetector(@ApplicationContext context: Context): YoloV6ActivityDetector{
        return YoloV6ActivityDetector(context)
    }

    @Singleton
    @Provides
    fun provideGestureDetector(@ApplicationContext context: Context): GestureDetector {
        return GestureDetector(context)
    }

    @Singleton
    @Provides
    fun provideEmotionDetector(@ApplicationContext context: Context): EmotionDetector {
        return EmotionDetector(context)
    }

    @Singleton
    @Provides
    fun provideThreatDetector(@ApplicationContext context: Context): ThreatDetector {
        return ThreatDetector(context)
    }

}