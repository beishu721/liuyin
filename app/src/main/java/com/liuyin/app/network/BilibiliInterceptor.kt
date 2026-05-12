package com.liuyin.app.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BilibiliInterceptor @Inject constructor(
    private val auth: BilibiliAuth
) : Interceptor {

    private val buvid3: String = BilibiliAuth.generateBuvid3()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        val bnut = System.currentTimeMillis().toString()
        var builder = request.newBuilder()
            .addHeader("User-Agent", UA)
            .addHeader("Referer", REFERER)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .addHeader("Origin", "https://www.bilibili.com")
            .addHeader("Cookie", "buvid3=$buvid3; b_nut=$bnut")

        if (shouldSign(url.encodedPath) && auth.isReady) {
            val params = url.queryParameterNames.associateWith {
                url.queryParameter(it) ?: ""
            }
            val signed = auth.sign(params)

            val newUrl = url.newBuilder()
                .encodedQuery(null)
                .apply {
                    signed.forEach { (k, v) -> addEncodedQueryParameter(k, v) }
                }
                .build()

            builder = builder.url(newUrl)
        } else if (shouldSign(url.encodedPath) && !auth.isReady) {
            Timber.w("WBI keys not ready, request to %s will go unsigned", url.encodedPath)
        }

        return chain.proceed(builder.build())
    }

    private fun shouldSign(path: String): Boolean {
        return !path.contains("wbi/index") && !path.contains("/nav")
    }

    companion object {
        private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
        private const val REFERER = "https://www.bilibili.com"
    }
}