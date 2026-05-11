package com.liuyin.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.liuyin.app.network.BilibiliAuth
import com.liuyin.app.network.api.BilibiliApi
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LiuYinApp : Application() {

    @Inject lateinit var api: BilibiliApi
    @Inject lateinit var auth: BilibiliAuth

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannel()

        scope.launch {
            try {
                val response = api.getWbiIndex()
                if (response.code == 0 && response.data?.wbiImg != null) {
                    val img = response.data.wbiImg
                    val imgKey = img.imgUrl.substringAfterLast("/").substringBefore(".")
                    val subKey = img.subUrl.substringAfterLast("/").substringBefore(".")
                    auth.updateKeys(imgKey, subKey)
                    Timber.d("WBI keys initialized on startup")
                }
            } catch (e: Exception) {
                Timber.w(e, "WBI key initialization failed, will retry on first request")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "播放控制",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playback"
    }
}