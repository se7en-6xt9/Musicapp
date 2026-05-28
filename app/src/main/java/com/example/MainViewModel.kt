package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(), // For Recently Played / Quick Picks
    val sections: Map<String, List<Song>> = emptyMap(),
    val currentSong: Song? = null
)

class MainViewModel : ViewModel() {
    private val repository = MusicRepository()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Fetch categories in parallel
            val lofiDeferred = async { repository.getSongsByQuery("lofi") }
            val arijitDeferred = async { repository.getSongsByQuery("arijit singh") }
            val haryanviDeferred = async { repository.getSongsByQuery("haryanvi") }
            val punjabiDeferred = async { repository.getSongsByQuery("punjabi") }
            
            val lofiSongs = lofiDeferred.await()
            val arijitSongs = arijitDeferred.await()
            val haryanviSongs = haryanviDeferred.await()
            val punjabiSongs = punjabiDeferred.await()

            val sections = buildMap {
                put("Arijit Singh", arijitSongs)
                put("Lofi Beats", lofiSongs)
                put("Haryanvi Hits", haryanviSongs)
                put("Punjabi Tracks", punjabiSongs)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songs = lofiSongs.take(4),
                sections = sections,
                currentSong = lofiSongs.firstOrNull()
            )
        }
    }

    fun playSong(song: Song) {
        _uiState.value = _uiState.value.copy(currentSong = song)
    }
}
