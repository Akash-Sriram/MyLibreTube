package com.github.libretube.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.libretube.helpers.BackupHelper

class AutoBackupWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        BackupHelper.runAutoBackup(applicationContext)
        return Result.success()
    }
}
