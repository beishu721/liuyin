package com.liuyin.app.data

import android.content.Context
import android.os.Environment
import com.liuyin.app.data.model.AudioInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val musicDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "LiuYin"
        ).also { it.mkdirs() }

    private val pictureDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "LiuYin"
        ).also { it.mkdirs() }

    suspend fun downloadCover(audioInfo: AudioInfo): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = audioInfo.cover
                if (url.isBlank()) return@withContext null

                val ext = url.substringAfterLast(".").substringBefore("?")
                    .ifBlank { "jpg" }
                val file = File(pictureDir, "${sanitizeFileName(audioInfo.title)}_cover.$ext")

                if (file.exists()) {
                    Timber.d("Cover already downloaded: ${file.absolutePath}")
                    return@withContext file
                }

                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Cover saved: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Timber.w(e, "Failed to download cover")
                null
            }
        }
    }

    suspend fun downloadAudio(audioInfo: AudioInfo): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = audioInfo.audioUrl
                if (url.isBlank()) return@withContext null

                val ext = audioInfo.audioUrl.substringAfterLast(".").substringBefore("?")
                    .ifBlank { "m4a" }
                val file = File(musicDir, "${sanitizeFileName(audioInfo.title)}.$ext")

                if (file.exists()) {
                    Timber.d("Audio already downloaded: ${file.absolutePath}")
                    return@withContext file
                }

                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Audio saved: ${file.absolutePath} (${file.length()} bytes)")
                file
            } catch (e: Exception) {
                Timber.w(e, "Failed to download audio")
                null
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(100)
            .trim()
    }
}
