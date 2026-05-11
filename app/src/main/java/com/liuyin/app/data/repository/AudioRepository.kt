package com.liuyin.app.data.repository

import com.liuyin.app.data.model.AudioInfo
import com.liuyin.app.data.model.VideoInfo
import com.liuyin.app.network.parser.BiliParserV1
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val parser: BiliParserV1
) {
    suspend fun getAudioInfo(bvid: String): AudioInfo = parser.parse(bvid)
    suspend fun fetchVideoInfo(bvid: String): VideoInfo = parser.getVideoInfo(bvid)
}