package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.Song
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(viewModel: MainViewModel, mediaController: MediaController?) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.search(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search songs, artists...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceCard,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.searchResults.isNotEmpty()) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.searchResults.isNotEmpty()) {
                    items(500) { index ->
                        val song = uiState.searchResults[index % uiState.searchResults.size]
                        SquareSongItem(song = song) {
                            viewModel.playSong(song, uiState.searchResults)
                        }
                    }
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found.", color = TextSecondary)
            }
        }
    }
}

@Composable
fun LibraryScreen(viewModel: MainViewModel, mediaController: MediaController?) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Your Library",
            color = Primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.librarySongs.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.librarySongs) { song ->
                    ListSongItem(song = song) {
                        viewModel.playSong(song, uiState.librarySongs)
                    }
                }
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Primary)
             }
        }
    }
}

@Composable
fun SquareSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfacePlayer)
        ) {
            if (song.artworkUrl100 != null) {
                AsyncImage(
                    model = song.artworkUrl100.replace("100x100", "500x500"),
                    contentDescription = "Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PrimaryDark, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.trackName ?: "Unknown",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.artistName ?: "Unknown",
            color = TextTertiary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ListSongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfacePlayer, RoundedCornerShape(8.dp))
        ) {
            if (song.artworkUrl100 != null) {
                AsyncImage(
                    model = song.artworkUrl100,
                    contentDescription = "Artwork",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.trackName ?: "Unknown",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName ?: "Unknown",
                color = TextTertiary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FullScreenPlayer(viewModel: MainViewModel, mediaController: MediaController?, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSong = uiState.currentSong ?: return
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    val scrollState = rememberScrollState()

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
        }
        mediaController?.addListener(listener)
        isPlaying = mediaController?.isPlaying == true
        duration = mediaController?.duration?.coerceAtLeast(0) ?: 0L

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, mediaController?.currentPosition) {
        while (isPlaying) {
            currentPosition = mediaController?.currentPosition?.coerceAtLeast(0) ?: 0L
            duration = mediaController?.duration?.coerceAtLeast(0) ?: 0L
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryDark.copy(alpha = 0.5f), Background, Background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = currentSong.artistName ?: "Radio",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = {  }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong.artworkUrl100 != null) {
                    AsyncImage(
                        model = currentSong.artworkUrl100.replace("100x100", "500x500"),
                        contentDescription = "Artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(80.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.trackName ?: "Unknown",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.artistName ?: "Unknown",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { value ->
                    val newPosition = (value * duration).toLong()
                    mediaController?.seekTo(newPosition)
                    currentPosition = newPosition
                },
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary,
                    inactiveTrackColor = SurfacePlayer
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), color = TextTertiary, fontSize = 12.sp)
                Text(formatDuration(duration), color = TextTertiary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(42.dp))
                }
                IconButton(onClick = { 
                    mediaController?.let {
                        val pos = it.currentPosition
                        it.seekTo(maxOf(0, pos - 10000))
                    }
                }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = TextSecondary, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Primary, CircleShape)
                        .clickable {
                            mediaController?.let {
                                if (it.isPlaying) it.pause() else it.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Background,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { 
                    mediaController?.let {
                        val pos = it.currentPosition
                        val dur = it.duration
                        it.seekTo(minOf(dur, pos + 10000))
                    }
                }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = TextSecondary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Suggestions List
            if (uiState.suggestedSongs.isNotEmpty()) {
                Text("Playing Next", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    uiState.suggestedSongs.forEach { suggestion ->
                        ListSongItem(song = suggestion) {
                            viewModel.playSong(suggestion, uiState.suggestedSongs)
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%d:%02d", minutes, seconds)
}
