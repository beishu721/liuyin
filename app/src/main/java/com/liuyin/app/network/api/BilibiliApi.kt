package com.liuyin.app.network.api

import com.liuyin.app.data.model.PlayUrlResponse
import com.liuyin.app.data.model.VideoInfoResponse
import com.liuyin.app.data.model.WbiIndexResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BilibiliApi {

    @GET("x/web-interface/view")
    suspend fun getVideoInfo(@Query("bvid") bvid: String): VideoInfoResponse

    @GET("x/player/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 64
    ): PlayUrlResponse

    @GET("x/web-interface/wbi/index")
    suspend fun getWbiIndex(): WbiIndexResponse
}