package com.liuyin.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.liuyin.app.data.model.AudioInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    companion object {
        private val PLAYLIST_KEY = stringPreferencesKey("playlist_history")
        private const val MAX_ITEMS = 50
    }

    val playlist: Flow<List<AudioInfo>> = dataStore.data
        .map { prefs -> deserialize(prefs[PLAYLIST_KEY]) }
        .catch { e ->
            Timber.w(e, "Failed to read playlist")
            emit(emptyList())
        }

    suspend fun addItem(audio: AudioInfo) {
        dataStore.edit { prefs ->
            val current = deserialize(prefs[PLAYLIST_KEY]).toMutableList()
            current.removeAll { it.audioUrl == audio.audioUrl }
            current.add(0, audio)
            prefs[PLAYLIST_KEY] = serialize(current.take(MAX_ITEMS))
        }
    }

    suspend fun removeItem(audio: AudioInfo) {
        dataStore.edit { prefs ->
            val current = deserialize(prefs[PLAYLIST_KEY]).toMutableList()
            current.removeAll { it.audioUrl == audio.audioUrl }
            prefs[PLAYLIST_KEY] = serialize(current)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.remove(PLAYLIST_KEY) }
    }

    private fun serialize(list: List<AudioInfo>): String = gson.toJson(list)
    private fun deserialize(json: String?): List<AudioInfo> =
        if (json.isNullOrBlank()) emptyList()
        else try {
            gson.fromJson(json, Array<AudioInfo>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
}
