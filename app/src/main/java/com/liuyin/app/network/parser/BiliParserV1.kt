package com.liuyin.app.network.parser

import com.liuyin.app.data.model.AudioInfo
import com.liuyin.app.data.model.VideoInfo
import com.liuyin.app.network.BilibiliAuth
import com.liuyin.app.network.api.BilibiliApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliParserV1 @Inject constructor(
    private val api: BilibiliApi,
    private val auth: BilibiliAuth
) : ParserPlugin {

    override suspend fun parse(bvid: String): AudioInfo {
        ensureKeys()

        val videoResponse = api.getVideoInfo(bvid)
        if (videoResponse.code != 0) {
            throw ParserException("视频信息获取失败: ${videoResponse.message}")
        }
        val videoData = videoResponse.data
            ?: throw ParserException("视频数据为空")

        val playResponse = api.getPlayUrl(bvid, videoData.cid)
        if (playResponse.code != 0) {
            throw ParserException("音频流获取失败: ${playResponse.message}")
        }
        val playData = playResponse.data
            ?: throw ParserException("播放数据为空")
        val audioList = playData.dash?.audio
            ?: throw ParserException("无音频流")

        val bestAudio = audioList.maxByOrNull { it.bandwidth }
            ?: throw ParserException("无可用音频流")

        return AudioInfo(
            title = videoData.title,
            cover = videoData.pic,
            audioUrl = bestAudio.baseUrl,
            backupUrls = bestAudio.backupUrls ?: emptyList(),
            duration = bestAudio.duration.toLong()
        )
    }

    override suspend fun getVideoInfo(bvid: String): VideoInfo {
        ensureKeys()

        val response = api.getVideoInfo(bvid)
        if (response.code != 0) {
            throw ParserException("视频信息获取失败: ${response.message}")
        }
        val data = response.data
            ?: throw ParserException("视频数据为空")

        return VideoInfo(
            bvid = data.bvid,
            title = data.title,
            cover = data.pic,
            duration = data.duration
        )
    }

    private suspend fun ensureKeys() {
        if (auth.isReady) return
        Timber.d("Fetching WBI keys")
        val response = api.getWbiIndex()
        if (response.code != 0 || response.data?.wbiImg == null) {
            Timber.w("Failed to fetch WBI keys, continuing without signing")
            return
        }
        val img = response.data.wbiImg
        val imgKey = extractKey(img.imgUrl)
        val subKey = extractKey(img.subUrl)
        if (imgKey != null && subKey != null) {
            auth.updateKeys(imgKey, subKey)
            Timber.d("WBI keys updated")
        }
    }

    /**
     * Extracts the key from a URL like:
     * https://i0.hdslb.com/bfs/wbi/7cd16881f280e3160c73f25f12e51085.png
     */
    private fun extractKey(url: String): String? {
        val name = url.substringAfterLast("/").substringBefore(".")
        return name.ifEmpty { null }
    }
}

class ParserException(message: String, cause: Throwable? = null) : Exception(message, cause)