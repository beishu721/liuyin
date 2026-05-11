package com.liuyin.app.network.parser

import com.liuyin.app.data.model.AudioInfo
import com.liuyin.app.data.model.VideoInfo

interface ParserPlugin {
    suspend fun parse(bvid: String): AudioInfo
    suspend fun getVideoInfo(bvid: String): VideoInfo
}