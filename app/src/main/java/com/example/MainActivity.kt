package com.example

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.ui.theme.*
import com.example.ui.*
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

enum class Screen { HOME, SEARCH, LIBRARY }

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private val mediaController = mutableStateOf<MediaController?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val currentScreen = remember { mutableStateOf(Screen.HOME) }
      val uiState by viewModel.uiState.collectAsState()

      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = { BottomNavigationBar(currentScreen.value) { currentScreen.value = it } }
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentScreen.value) {
                Screen.HOME -> MainContent(viewModel, mediaController.value)
                Screen.SEARCH -> SearchScreen(viewModel, mediaController.value)
                Screen.LIBRARY -> LibraryScreen(viewModel, mediaController.value)
            }
            MiniPlayer(
              viewModel = viewModel,
              mediaController = mediaController.value,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
            )
          }
        }
        if (uiState.isFullScreenPlayerOpen) {
           FullScreenPlayer(viewModel = viewModel, mediaController = mediaController.value, onClose = { viewModel.setFullScreenPlayerOpen(false) })
        }
      }
    }
  }

  override fun onStart() {
      super.onStart()
      val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
      controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
      controllerFuture?.addListener(
          { mediaController.value = controllerFuture?.get() },
          MoreExecutors.directExecutor()
      )
  }

  override fun onStop() {
      super.onStop()
      controllerFuture?.let { MediaController.releaseFuture(it) }
  }
}

@Composable
fun MainContent(viewModel: MainViewModel, mediaController: MediaController?) {
  val uiState by viewModel.uiState.collectAsState()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(Background),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
  ) {
    item {
      TopNavigation()
    }
    item {
      FeaturedCard(
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 16.dp)
      )
    }
    item {
      QuickPicksSection(
        songs = uiState.songs,
        onSongClick = { song -> 
            viewModel.playSong(song, uiState.songs)
        },
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }
    uiState.sections.forEach { (title, sectionSongs) ->
        if (sectionSongs.isNotEmpty()) {
            item {
                HorizontalSongSection(
                    title = title,
                    songs = sectionSongs,
                    onSongClick = { song -> 
                        viewModel.playSong(song, sectionSongs)
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
  }
}

@Composable
fun TopNavigation() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "VibeMusic",
      color = Primary,
      fontSize = 24.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = (-0.5).sp
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      IconButton(
        onClick = { },
        modifier = Modifier
          .size(44.dp)
          .background(SurfaceCard, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Outlined.Search,
          contentDescription = "Search",
          tint = TextPrimary
        )
      }
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(SurfaceCard, CircleShape)
          .clickable { },
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(
              brush = Brush.topRightEndToBottomLeftStart(listOf(Primary, PrimaryDark)),
              shape = CircleShape
            )
        )
      }
    }
  }
}

private fun Brush.Companion.topRightEndToBottomLeftStart(colors: List<Color>) =
  Brush.linearGradient(colors = colors)

@Composable
fun FeaturedCard(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(16f / 10f)
      .clip(RoundedCornerShape(24.dp))
  ) {
    // Background glow/blur decoration simulation
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = 40.dp, y = (-40).dp)
        .size(200.dp)
        .background(Primary.copy(alpha = 0.2f), CircleShape)
        .blur(40.dp)
    )

    // Overlay colors
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(PrimaryDark.copy(alpha = 0.4f))
    )

    // Bottom gradient
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, Background),
            startY = 50f
          )
        )
    )

    // Content
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(24.dp)
    ) {
      Text(
        text = "NEW RELEASE",
        color = Primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Midnight\nEchos",
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Synthesized Souls",
        color = TextSecondary,
        fontSize = 14.sp
      )
    }
  }
}

@Composable
fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = "Recently Played",
      color = TextSecondary,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))

    if (songs.isNotEmpty()) {
        val pairs = songs.take(4).chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            pairs.forEach { rowSongs ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowSongs.forEach { song ->
                        QuickPickItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Handle odd numbers
                    if (rowSongs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Primary)
    }
  }
}

@Composable
fun QuickPickItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(SurfaceCard)
      .clickable { onClick() }
      .padding(8.dp),
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
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.trackName ?: "Unknown",
        color = TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = song.artistName ?: "Unknown",
        color = TextTertiary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun MiniPlayer(
    viewModel: MainViewModel,
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val currentSong = uiState.currentSong
  var isPlaying by remember { mutableStateOf(false) }

  DisposableEffect(mediaController) {
      val listener = object : Player.Listener {
          override fun onIsPlayingChanged(isPlayingChange: Boolean) {
              isPlaying = isPlayingChange
          }
          override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
              if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                  viewModel.playNext()
              }
          }
          override fun onPlaybackStateChanged(playbackState: Int) {
              if (playbackState == Player.STATE_ENDED) {
                  viewModel.playNext()
              }
          }
      }
      mediaController?.addListener(listener)
      isPlaying = mediaController?.isPlaying == true

      onDispose {
          mediaController?.removeListener(listener)
      }
  }

  // Update MediaController when currentSong changes
  LaunchedEffect(currentSong) {
      if (currentSong != null && currentSong.previewUrl != null) {
          mediaController?.let { controller ->
             val mediaItem = MediaItem.fromUri(currentSong.previewUrl)
             if (controller.currentMediaItem?.localConfiguration?.uri?.toString() != currentSong.previewUrl) {
                 controller.setMediaItem(mediaItem)
                 controller.prepare()
                 controller.play()
             }
          }
      }
  }

  if (currentSong == null) return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(SurfacePlayer)
      .clickable { viewModel.setFullScreenPlayerOpen(true) }
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(Primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
         if (currentSong.artworkUrl100 != null) {
            AsyncImage(
                model = currentSong.artworkUrl100,
                contentDescription = "Artwork",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
            )
         } else {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = null,
              tint = PrimaryDark,
              modifier = Modifier.size(20.dp)
            )
         }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = currentSong.trackName ?: "Unknown",
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = currentSong.artistName ?: "Unknown",
          color = TextSecondary,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { 
            mediaController?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "Play/Pause",
            tint = TextPrimary
          )
        }
        IconButton(onClick = { viewModel.playNext() }) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Next",
            tint = TextPrimary
          )
        }
      }
    }
  }
}

@Composable
fun BottomNavigationBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
  NavigationBar(
    containerColor = SurfaceNav,
    contentColor = TextSecondary,
    tonalElevation = 0.dp,
    modifier = Modifier.height(80.dp)
  ) {
    NavigationBarItem(
      icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
      label = { Text("HOME", fontSize = 10.sp, letterSpacing = 0.5.sp) },
      selected = currentScreen == Screen.HOME,
      onClick = { onScreenSelected(Screen.HOME) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Primary,
        selectedTextColor = TextPrimary,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary,
        indicatorColor = SurfaceChip
      )
    )
    NavigationBarItem(
      icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
      label = { Text("SEARCH", fontSize = 10.sp, letterSpacing = 0.5.sp) },
      selected = currentScreen == Screen.SEARCH,
      onClick = { onScreenSelected(Screen.SEARCH) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Primary,
        selectedTextColor = TextPrimary,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary,
        indicatorColor = SurfaceChip
      )
    )
    NavigationBarItem(
      icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") },
      label = { Text("LIBRARY", fontSize = 10.sp, letterSpacing = 0.5.sp) },
      selected = currentScreen == Screen.LIBRARY,
      onClick = { onScreenSelected(Screen.LIBRARY) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Primary,
        selectedTextColor = TextPrimary,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary,
        indicatorColor = SurfaceChip
      )
    )
  }
}

@Composable
fun HorizontalSongSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (songs.isNotEmpty()) {
                items(500) { index ->
                    val song = songs[index % songs.size]
                    SongCard(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfacePlayer)
        ) {
            if (song.artworkUrl100 != null) {
                AsyncImage(
                    model = song.artworkUrl100,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.trackName ?: "Unknown",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artistName ?: "Unknown",
            color = TextTertiary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}