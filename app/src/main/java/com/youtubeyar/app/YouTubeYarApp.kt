package com.youtubeyar.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.youtubeyar.app.work.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class YouTubeYarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleDailyReminder()
    }

    /**
     * یادآور روزانه با WorkManager زمان‌بندی می‌شود.
     * خودِ Worker داخل اجرا تنظیمات را چک می‌کند و اگر یادآور خاموش باشد، نوتیفیکیشن نشان نمی‌دهد.
     */
    private fun scheduleDailyReminder() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
