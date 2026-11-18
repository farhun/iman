package com.imn.iman.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.imn.iman.data.repository.PrayerTimeRepository
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.notifications.PrayerNotificationScheduler
import com.imn.iman.widget.WidgetRefresher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.concurrent.TimeUnit

object PrayerRefreshWork {
    const val UNIQUE_WORK_NAME = "prayer_times_refresh"
    private const val TAG = "ImanWidget"

    suspend fun enqueueIfNeeded(context: Context, settings: UserSettings? = null) {
        val appContext = context.applicationContext
        try {
            val settingsRepository = SettingsRepository(appContext)
            val currentSettings = settings ?: settingsRepository.settingsFlow.first()
            val repository = PrayerTimeRepository(settingsRepository)
            if (!repository.usesApiSource(currentSettings)) return

            val today = Date()
            if (repository.hasOnlinePrayerDateTimes(today, currentSettings)) return

            val retryAt = repository.getProvisionalRetryAt(today, currentSettings)
            val initialDelay = retryAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) } ?: 0L
            val request = OneTimeWorkRequestBuilder<PrayerRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Worker enqueued name=$UNIQUE_WORK_NAME initialDelay=${initialDelay}ms")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Could not enqueue prayer-time refresh worker", error)
        }
    }
}

class PrayerRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started attempt=$runAttemptCount")
        var resultLabel = "unknown"
        try {
            val settingsRepository = SettingsRepository(applicationContext)
            val settings = settingsRepository.settingsFlow.first()
            val repository = PrayerTimeRepository(settingsRepository)
            if (!repository.usesApiSource(settings)) {
                resultLabel = "success"
                Log.d(TAG, "Retry decision=success reason=non_api_source")
                return Result.success()
            }

            val today = Date()
            val previousTimes = repository.getPrayerDateTimesNoNetwork(today, settings)
            val result = repository.refreshPrayerTimesOnline(today, settings)
            if (result.source == com.imn.iman.data.repository.TimesSource.ONLINE &&
                prayerTimesDifferByMinute(previousTimes, result.times)
            ) {
                WidgetRefresher.refreshAll(applicationContext, "worker")
                PrayerNotificationScheduler(applicationContext, repository)
                    .syncNotifications(settings, noNetwork = true)
            }

            val shouldRetry = result.provisional && runAttemptCount < 5
            Log.d(
                TAG,
                "Retry decision=${if (shouldRetry) "retry" else "success"} provisional=${result.provisional} attempt=$runAttemptCount nextRetryAt=${result.nextRetryAt}"
            )
            resultLabel = if (shouldRetry) "retry" else "success"
            return if (shouldRetry) Result.retry() else Result.success()
        } catch (cancelled: CancellationException) {
            resultLabel = "cancelled"
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Worker failed safely attempt=$runAttemptCount", error)
            resultLabel = "retry"
            return Result.retry()
        } finally {
            Log.d(TAG, "Worker finished result=$resultLabel attempt=$runAttemptCount")
        }
    }

    private fun prayerTimesDifferByMinute(previous: Map<String, Date>, current: Map<String, Date>): Boolean =
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").any { prayer ->
            val oldTime = previous[prayer]?.time ?: return@any true
            val newTime = current[prayer]?.time ?: return@any true
            kotlin.math.abs(oldTime - newTime) >= TimeUnit.MINUTES.toMillis(1)
        }

    private companion object {
        const val TAG = "ImanWidget"
    }
}
