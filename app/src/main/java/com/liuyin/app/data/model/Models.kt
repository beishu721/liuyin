package com.liuyin.app.data.model

import com.google.gson.annotations.SerializedName

// === Bilibili API Response Models ===

data class VideoInfoResponse(
    val code: Int,
    val data: VideoData?,
    val message: String?
)

data class VideoData(
    val bvid: String,
    val aid: Long,
    val title: String,
    val pic: String,
    val cid: Long,
    val duration: Long,
    val pages: List<PageInfo>?
)

data class PageInfo(
    val cid: Long,
    val page: Int,
    val part: String
)

data class PlayUrlResponse(
    val code: Int,
    val data: PlayUrlData?,
    val message: String?
)

data class PlayUrlData(
    val dash: DashInfo?,
    val quality: Int?
)

data class DashInfo(
    val audio: List<AudioStream>?
)

data class AudioStream(
    val id: Long,
    val baseUrl: String,
    @SerializedName("backupUrl")
    val backupUrls: List<String>?,
    val bandwidth: Long,
    val mimeType: String,
    val codecs: String,
    val duration: Float
)

data class WbiIndexResponse(
    val code: Int,
    val data: WbiData?
)

data class WbiData(
    @SerializedName("wbi_img")
    val wbiImg: WbiImg?
)

data class WbiImg(
    @SerializedName("img_url")
    val imgUrl: String,
    @SerializedName("sub_url")
    val subUrl: String
)

// === Domain Models ===

data class AudioInfo(
    val title: String,
    val cover: String,
    val audioUrl: String,
    val backupUrls: List<String>,
    val duration: Long
)

data class VideoInfo(
    val bvid: String,
    val title: String,
    val cover: String,
    val duration: Long
)