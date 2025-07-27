package com.example.myapplication4.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.myapplication4.domain.usecase.SyncOfflineFacesUseCase
import android.util.Log

@HiltWorker
class FaceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncOfflineFacesUseCase: SyncOfflineFacesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("FaceSyncWorker", "Starting background face sync.")
        return try {
            val success = syncOfflineFacesUseCase()
            if (success) {
                Log.d("FaceSyncWorker", "Face sync completed successfully.")
                Result.success()
            } else {
                Log.d("FaceSyncWorker", "Face sync completed with pending items or no network.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("FaceSyncWorker", "Error during face sync: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "FaceSyncWorker"
    }
}