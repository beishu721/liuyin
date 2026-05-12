package com.liuyin.app.network.api

import com.liuyin.app.data.model.PlayUrlResponse
import com.liuyin.app.data.model.VideoInfoResponse
import com.liuyin.app.data.model.WbiIndexResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BilibiliApi {

    @GET("x/web-interface/view")
    suspend fun getVideoInfo(
        @Query("bvid") bvid: String,
        @Query("platform") platform: String = "web"
    ): VideoInfoResponse

    @GET("x/player/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("platform") platform: String = "web",
        @Query("qn") qn: Int = 0,
        @Query("fnval") fnval: Int = 4048,
        @Query("fnver") fnver: Int = 0
    ): PlayUrlResponse

    @GET("x/web-interface/nav")
    suspend fun getWbiIndex(
        @Query("platform") platform: String = "web"
    ): WbiIndexResponse
}