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
    val currentSong: Song? = null,
    val currentLyrics: String? = null,
    val suggestedSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val librarySongs: List<Song> = emptyList(),
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isFullScreenPlayerOpen: Boolean = false
)

class MainViewModel : ViewModel() {
    private val repository = MusicRepository()
    private var searchJob: kotlinx.coroutines.Job? = null
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
            val devotionalDeferred = async { repository.getSongsByQuery("bhakti") }
            val partyDeferred = async { repository.getSongsByQuery("party") }
            val sadDeferred = async { repository.getSongsByQuery("sad") }
            val libraryDeferred = async { repository.getSongsByQuery("relax") }
            
            val lofiSongs = lofiDeferred.await()
            val arijitSongs = arijitDeferred.await()
            val haryanviSongs = haryanviDeferred.await()
            val punjabiSongs = punjabiDeferred.await()
            val devotionalSongs = devotionalDeferred.await()
            val partySongs = partyDeferred.await()
            val sadSongs = sadDeferred.await()
            val librarySongs = libraryDeferred.await()

            val sections = buildMap {
                put("Party Bangers", partySongs)
                put("Arijit Singh", arijitSongs)
                put("Lofi Beats", lofiSongs)
                put("Punjabi Tracks", punjabiSongs)
                put("Sad Songs", sadSongs)
                put("Haryanvi Hits", haryanviSongs)
                put("Devotional", devotionalSongs)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songs = lofiSongs.take(4),
                sections = sections,
                librarySongs = librarySongs,
                currentSong = lofiSongs.firstOrNull(),
                queue = lofiSongs,
                currentIndex = 0
            )
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.getSongsByQuery(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun playSong(song: Song, contextQueue: List<Song> = emptyList()) {
        val newQueue = if (contextQueue.isNotEmpty()) contextQueue else _uiState.value.queue
        val index = newQueue.indexOf(song).takeIf { it != -1 } ?: 0
        _uiState.value = _uiState.value.copy(
            currentSong = song,
            currentLyrics = null,
            suggestedSongs = emptyList(),
            queue = if (contextQueue.isNotEmpty()) contextQueue else newQueue + (if (contextQueue.isEmpty() && !_uiState.value.queue.contains(song)) listOf(song) else emptyList()),
            currentIndex = index
        )
        fetchLyricsAndSuggestions(song)
    }

    private fun fetchLyricsAndSuggestions(song: Song) {
        viewModelScope.launch {
            val query = song.artistName ?: song.trackName ?: "music"
            val suggestions = repository.getSongsByQuery(query).filter { it.trackId != song.trackId }.shuffled().take(10)
            _uiState.value = _uiState.value.copy(suggestedSongs = suggestions)
        }
        viewModelScope.launch {
            val lyrics = repository.getLyrics(song.trackId.toString()) // Track ID string but might need alphanumeric ID. We'll use songname mock if fail.
            _uiState.value = _uiState.value.copy(currentLyrics = lyrics ?: "Lyrics are not available for this song yet.")
        }
    }

    fun playNext() {
        val state = _uiState.value
        if (state.queue.isNotEmpty()) {
            val nextIndex = (state.currentIndex + 1) % state.queue.size
            playSong(state.queue[nextIndex], state.queue)
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.queue.isNotEmpty()) {
            val prevIndex = if (state.currentIndex - 1 < 0) state.queue.size - 1 else state.currentIndex - 1
            playSong(state.queue[prevIndex], state.queue)
        }
    }

    fun setFullScreenPlayerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreenPlayerOpen = isOpen)
    }
}
