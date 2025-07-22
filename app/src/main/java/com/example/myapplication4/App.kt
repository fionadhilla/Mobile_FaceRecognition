package com.example.myapplication4

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.util.Log

@HiltAndroidApp
class MobileFaceRecognition : Application(), Configuration.Provider{
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("WorkManagerConfig", "HiltWorkerFactory is being used (Manual Config via Configuration.Provider)!")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }
}