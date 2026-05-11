package com.liuyin.app.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.liuyin.app.data.model.AudioInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiuYinPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioInfo?>(null)
    val currentAudio: StateFlow<AudioInfo?> = _currentAudio.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var backupUrls: List<String> = emptyList()
    private var currentBackupIndex = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.LOADING
                    Player.STATE_READY -> PlaybackState.PLAYING
                    Player.STATE_ENDED -> PlaybackState.COMPLETED
                    else -> PlaybackState.IDLE
                }
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.takeIf { it > 0 } ?: 0L
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && player.playbackState == Player.STATE_READY) {
                    _playbackState.value = PlaybackState.PAUSED
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Timber.w(error, "Playback error, trying backup URL #$currentBackupIndex")
                if (currentBackupIndex < backupUrls.size) {
                    val url = backupUrls[currentBackupIndex]
                    currentBackupIndex++
                    player.stop()
                    player.setMediaItem(MediaItem.fromUri(url))
                    player.prepare()
                    player.play()
                } else {
                    _playbackState.value = PlaybackState.ERROR
                }
            }
        })

        scope.launch {
            while (true) {
                if (player.isPlaying) {
                    _position.value = player.currentPosition
                }
                delay(250)
            }
        }
    }

    fun play(audioInfo: AudioInfo) {
        _currentAudio.value = audioInfo
        backupUrls = audioInfo.backupUrls
        currentBackupIndex = 0

        context.startForegroundService(Intent(context, PlaybackService::class.java))

        player.setMediaItem(MediaItem.fromUri(audioInfo.audioUrl))
        player.prepare()
        player.play()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun stop() {
        player.stop()
        _playbackState.value = PlaybackState.IDLE
        _currentAudio.value = null
        _position.value = 0
    }

    fun release() {
        player.release()
    }

    enum class PlaybackState {
        IDLE, LOADING, PLAYING, PAUSED, ERROR, COMPLETED
    }
}