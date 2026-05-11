package com.liuyin.app.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BilibiliInterceptor @Inject constructor(
    private val auth: BilibiliAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        var builder = request.newBuilder()
            .addHeader("User-Agent", UA)
            .addHeader("Referer", REFERER)

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
        }

        return chain.proceed(builder.build())
    }

    private fun shouldSign(path: String): Boolean {
        return !path.contains("wbi/index")
    }

    companion object {
        private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val REFERER = "https://www.bilibili.com"
    }
}