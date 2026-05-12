package com.liuyin.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liuyin.app.data.DownloadUtil
import com.liuyin.app.network.BilibiliAuth
import com.liuyin.app.data.repository.AudioRepository
import com.liuyin.app.data.repository.PlaylistRepository
import com.liuyin.app.player.LiuYinPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AudioRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadUtil: DownloadUtil,
    private val auth: BilibiliAuth,
    val player: LiuYinPlayer
) : ViewModel() {

    private val _downloadMsg = MutableStateFlow<String?>(null)
    val downloadMsg: StateFlow<String?> = _downloadMsg.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    fun updateInput(text: String) {
        _inputText.value = text
        _error.value = null
    }

    fun onPlayClicked() {
        val bvid = extractBvid(_inputText.value)
        if (bvid == null) {
            _error.value = "请输入有效的BV号或B站视频链接"
            return
        }
        parseAndPlay(bvid)
    }

    fun playBvid(bvid: String) {
        _inputText.value = bvid
        parseAndPlay(bvid)
    }

    private fun parseAndPlay(bvid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val audioInfo = repository.getAudioInfo(bvid)
                player.play(audioInfo)
                playlistRepository.addItem(audioInfo)
                // 后台下载封面和音频
                launch { downloadCoverAndAudio(audioInfo) }
            } catch (e: Exception) {
                val wbiStatus = if (auth.isReady) "签名已就绪" else "WBI密钥未获取"
                val detail = "[${e.javaClass.simpleName}] ${e.message ?: "无详细信息"} | $wbiStatus"
                _error.value = when {
                    e.message?.contains("视频信息获取失败") == true -> e.message
                    e.message?.contains("音频流获取失败") == true -> e.message
                    e.message?.contains("无音频流") == true -> "该视频没有可用的音频流"
                    e.message?.contains("无可用音频流") == true -> "没有可播放的音频流"
                    else -> "播放失败: $detail"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showPlayer() { _showFullPlayer.value = true }
    fun hidePlayer() { _showFullPlayer.value = false }

    fun clearError() { _error.value = null }
    fun clearDownloadMsg() { _downloadMsg.value = null }

    private suspend fun downloadCoverAndAudio(audioInfo: com.liuyin.app.data.model.AudioInfo) {
        val coverFile = downloadUtil.downloadCover(audioInfo)
        val audioFile = downloadUtil.downloadAudio(audioInfo)
        val parts = mutableListOf<String>()
        if (coverFile != null) parts.add("封面已保存")
        if (audioFile != null) parts.add("音频已保存")
        if (parts.isNotEmpty()) {
            _downloadMsg.value = parts.joinToString("，")
        }
    }

    companion object {
        private val BV_REGEX = Regex("BV[a-zA-Z0-9]{10,}")

        fun extractBvid(input: String): String? {
            BV_REGEX.find(input.trim())?.value?.let { return it }
            val trimmed = input.trim()
            if (trimmed.length >= 12 && trimmed.startsWith("BV")) return trimmed
            return null
        }
    }
}
