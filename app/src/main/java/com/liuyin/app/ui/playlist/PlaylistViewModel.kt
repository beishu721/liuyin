package com.liuyin.app.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liuyin.app.data.model.AudioInfo
import com.liuyin.app.data.repository.PlaylistRepository
import com.liuyin.app.player.LiuYinPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val player: LiuYinPlayer
) : ViewModel() {

    val playlist: StateFlow<List<AudioInfo>> = playlistRepository.playlist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun playFromHistory(audio: AudioInfo) {
        player.play(audio)
        viewModelScope.launch { playlistRepository.addItem(audio) }
    }

    fun removeFromHistory(audio: AudioInfo) {
        viewModelScope.launch { playlistRepository.removeItem(audio) }
    }

    fun clearHistory() {
        viewModelScope.launch { playlistRepository.clearAll() }
    }
}
