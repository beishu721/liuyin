package com.liuyin.app.network

import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BilibiliAuth @Inject constructor() {

    @Volatile
    private var mixKey: String? = null

    val isReady: Boolean get() = mixKey != null

    fun updateKeys(imgKey: String, subKey: String) {
        mixKey = subKey.take(4) + imgKey.take(4)
    }

    fun sign(params: Map<String, String>): Map<String, String> {
        val mk = mixKey ?: return params

        val sorted = params.entries
            .filter { it.key != "w_rid" && it.key != "wts" }
            .sortedBy { it.key }

        val query = sorted.joinToString("") { "${it.key}${it.value}" }
        val toSign = query + mk
        val wts = (System.currentTimeMillis() / 1000).toInt().toString()
        val wrid = md5(toSign)

        return params + mapOf("wts" to wts, "w_rid" to wrid)
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun generateBuvid3(): String {
            val uuid = UUID.randomUUID().toString().uppercase()
            return "${uuid}infoc"
        }
    }
}