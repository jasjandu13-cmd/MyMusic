@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.mymusic

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.mymusic.DatabaseProvider
import com.example.mymusic.EqualizerManager
import com.example.mymusic.FavoriteSongEntity
import com.example.mymusic.LoudnessEnhancerManager
import com.example.mymusic.MusicRepository
import com.example.mymusic.PlaybackService
import com.example.mymusic.PlayerHolder
import com.example.mymusic.PlaylistEntity
import com.example.mymusic.PlaylistSongCrossRef
import com.example.mymusic.ReplayGainEntity
import com.example.mymusic.ReplayGainMode
import com.example.mymusic.Song
import com.example.mymusic.StoredSong
import com.example.mymusic.components.AlbumRow
import com.example.mymusic.components.ArtistRow
import com.example.mymusic.components.MiniPlayerBar
import com.example.mymusic.components.SongRow
import com.example.mymusic.dialogs.BoostDialog
import com.example.mymusic.dialogs.EqualizerDialog
import com.example.mymusic.dialogs.NowPlayingDialog
import com.example.mymusic.dialogs.QueueDialog
import com.example.mymusic.dialogs.ReplayGainDialog
import com.example.mymusic.dialogs.SleepTimerDialog
import com.example.mymusic.ArtworkRepository
import com.example.mymusic.getAudioPermission
import com.example.mymusic.utils.albumKey
import com.example.mymusic.utils.formatDuration
import com.example.mymusic.utils.gainToLinear
import com.example.mymusic.utils.toMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.startService(Intent(context, PlaybackService::class.java))
    }
    val scope = rememberCoroutineScope()
    val db = remember { DatabaseProvider.getDatabase(context) }
    val equalizerManager = remember { EqualizerManager() }
    val loudnessManager = remember { LoudnessEnhancerManager() }

    val permission = getAudioPermission()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<PlaylistEntity>>(emptyList()) }
    var selectedPlaylistSongs by remember { mutableStateOf<List<StoredSong>>(emptyList()) }

    var currentIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }

    var openedPlaylistId by remember { mutableLongStateOf(-1L) }
    var openedPlaylistName by remember { mutableStateOf("") }

    var showEqualizerDialog by remember { mutableStateOf(false) }
    var eqBandCount by remember { mutableIntStateOf(0) }
    var eqMinLevel by remember { mutableIntStateOf(-1500) }
    var eqMaxLevel by remember { mutableIntStateOf(1500) }
    val eqLevels = remember { mutableStateMapOf<Int, Float>() }

    var showBoostDialog by remember { mutableStateOf(false) }
    var preampLevel by remember { mutableFloatStateOf(1.0f) }
    var loudnessBoostMb by remember { mutableFloatStateOf(0f) }

    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    var currentArtwork by remember { mutableStateOf<Bitmap?>(null) }

    var showNowPlaying by remember { mutableStateOf(false) }

    var showQueueDialog by remember { mutableStateOf(false) }
    var queueVersion by remember { mutableIntStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }

    var favoriteSongIds by remember { mutableStateOf(setOf<Long>()) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingMs by remember { mutableLongStateOf(0L) }
    var sleepTimerActive by remember { mutableStateOf(false) }

    val player = remember(context) { PlayerHolder.get(context.applicationContext) }

    var replayGainMode by remember { mutableStateOf(ReplayGainMode.OFF) }
    var replayGainMap by remember { mutableStateOf<Map<Long, ReplayGainEntity>>(emptyMap()) }
    var showReplayGainDialog by remember { mutableStateOf(false) }
    var replayGainScanning by remember { mutableStateOf(false) }

    fun updateCurrentArtwork() {
        val currentSongItem = songs.getOrNull(player.currentMediaItemIndex)
        currentArtwork = currentSongItem?.let {
            ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri)
        }
    }

    fun applyReplayGainForCurrentSong() {
        val currentSong = songs.getOrNull(player.currentMediaItemIndex) ?: return
        val replay = replayGainMap[currentSong.id]

        if (replayGainMode == ReplayGainMode.OFF || replay == null) {
            player.volume = preampLevel.coerceIn(0f, 1f)
            loudnessManager.setTargetGain(loudnessBoostMb.toInt())
            return
        }

        val selectedGainDb = when (replayGainMode) {
            ReplayGainMode.TRACK -> replay.trackGainDb
            ReplayGainMode.ALBUM -> replay.albumGainDb ?: replay.trackGainDb
            ReplayGainMode.OFF -> null
        } ?: 0f

        val linear = gainToLinear(selectedGainDb)

        if (linear <= 1f) {
            player.volume = (linear * preampLevel).coerceIn(0f, 1f)
            loudnessManager.setTargetGain(0)
        } else {
            player.volume = preampLevel.coerceIn(0f, 1f)
            val boostDb = 20f * kotlin.math.log10(linear)
            val boostMillibels = (boostDb * 100f).toInt().coerceIn(0, 1500)
            loudnessManager.setTargetGain(boostMillibels)
        }
    }


    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
                duration = if (player.duration > 0) player.duration else 0L
                updateCurrentArtwork()
                applyReplayGainForCurrentSong()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = if (player.duration > 0) player.duration else 0L
            }
        }

        player.addListener(listener)

        onDispose {
            equalizerManager.release()
            loudnessManager.release()
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player, isPlaying, isSeeking) {
        while (isActive) {
            if (!isSeeking) {
                currentPosition = if (player.currentPosition > 0) player.currentPosition else 0L
                duration = if (player.duration > 0) player.duration else 0L
            }
            delay(500)
        }
    }

    suspend fun loadPlaylists() {
        playlists = db.playlistDao().getAllPlaylists()
    }

    fun setupAudioEffects() {
        val audioSessionId = player.audioSessionId
        if (audioSessionId != 0) {
            equalizerManager.setup(audioSessionId)
            loudnessManager.setup(audioSessionId)
            loudnessManager.setTargetGain(loudnessBoostMb.toInt())

            val bandCount = equalizerManager.getNumberOfBands().toInt()
            eqBandCount = bandCount

            val range = equalizerManager.getBandLevelRange()
            eqMinLevel = range[0].toInt()
            eqMaxLevel = range[1].toInt()

            eqLevels.clear()
            for (i in 0 until bandCount) {
                eqLevels[i] = equalizerManager.getBandLevel(i.toShort()).toFloat()
            }

            player.volume = preampLevel
        }
    }

    suspend fun loadFavorites() {
        favoriteSongIds = db.favoriteSongDao().getAllFavoriteSongIds().toSet()
    }

    fun toggleFavorite(songId: Long) {
        scope.launch {
            if (favoriteSongIds.contains(songId)) {
                db.favoriteSongDao().removeFavorite(songId)
            } else {
                db.favoriteSongDao().addFavorite(FavoriteSongEntity(songId))
            }
            loadFavorites()
        }
    }


    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = repeatMode
    }

    fun repeatLabel(): String {
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> "Repeat 1"
            Player.REPEAT_MODE_ALL -> "Repeat All"
            else -> "Repeat Off"
        }
    }

    suspend fun loadReplayGain() {
        replayGainMap = db.replayGainDao().getAllReplayGain().associateBy { it.songId }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    suspend fun scanReplayGainPlaceholder() {
        replayGainScanning = true
        val items = songs.map { song ->
            ReplayGainEntity(
                songId = song.id,
                trackGainDb = 0f,
                trackPeak = 1.0f,
                albumGainDb = 0f,
                albumPeak = 1.0f,
                albumKey = albumKey(song)
            )
        }
        db.replayGainDao().upsertReplayGain(items)
        loadReplayGain()
        replayGainScanning = false
    }

    val filteredSongs = remember(songs, searchQuery) {
        val query = searchQuery.trim().lowercase(Locale.getDefault())

        if (query.isBlank()) {
            emptyList()
        } else {
            songs.filter { song ->
                song.title.lowercase(Locale.getDefault()).contains(query) ||
                        song.artist.lowercase(Locale.getDefault()).contains(query) ||
                        song.album.lowercase(Locale.getDefault()).contains(query)
            }
        }
    }

    val favoriteSongs = remember(songs, favoriteSongIds) {
        songs.filter { favoriteSongIds.contains(it.id) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            // 1) Heavy work off the main thread
            val loadedSongs = withContext(Dispatchers.IO) {
                MusicRepository.getSongs(context)
            }

            // 2) Back on main: update state and DB-backed stuff
            songs = loadedSongs
            // This is a no-op right now, but keep it for later
            MusicRepository.cacheSongsInDatabase()
            loadPlaylists()
            loadFavorites()
            loadReplayGain()

            // 3) Set up the player queue
            val mediaItems = songs.map { it.toMediaItem(context) }
            player.setMediaItems(mediaItems)
            player.prepare()
            updateCurrentArtwork()
            player.shuffleModeEnabled = shuffleEnabled
            player.repeatMode = repeatMode
            setupAudioEffects()
        }
    }

    LaunchedEffect(sleepTimerActive, sleepTimerRemainingMs) {
        if (sleepTimerActive && sleepTimerRemainingMs > 0L) {
            while (sleepTimerActive && sleepTimerRemainingMs > 0L) {
                delay(1000)
                sleepTimerRemainingMs -= 1000L
            }

            if (sleepTimerActive) {
                player.pause()
                sleepTimerRemainingMs = 0L
                sleepTimerActive = false
            }
        }
    }

    val albums = remember(songs) {
        songs
            .groupBy { it.album.ifBlank { "Unknown Album" } }
            .toList()
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    val artists = remember(songs) {
        songs
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .toList()
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    val genres = remember(songs) {
        songs.groupBy { it.genre.ifBlank { "Unknown Genre" } }
            .toList()
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    val folders = remember(songs) {
        songs.groupBy { it.folder.ifBlank { "Unknown Folder" } }
            .toList()
            .sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    val currentSong = songs.getOrNull(currentIndex)

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                playlistName = ""
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            scope.launch {
                                db.playlistDao().insertPlaylist(
                                    PlaylistEntity(name = playlistName.trim())
                                )
                                playlistName = ""
                                loadPlaylists()
                                showCreatePlaylistDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistDialog = false
                        playlistName = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            }
        )
    }

    if (showAddToPlaylistDialog && songToAdd != null) {
        AlertDialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                songToAdd = null
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddToPlaylistDialog = false
                        songToAdd = null
                    }
                ) {
                    Text("Close")
                }
            },
            title = { Text("Add to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Create a playlist first.")
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            db.playlistDao().addSongToPlaylist(
                                                PlaylistSongCrossRef(
                                                    playlistId = playlist.id,
                                                    songId = songToAdd!!.id
                                                )
                                            )
                                            showAddToPlaylistDialog = false
                                            songToAdd = null
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        )
    }

    if (showEqualizerDialog) {
        EqualizerDialog(
            eqBandCount = eqBandCount,
            eqMinLevel = eqMinLevel,
            eqMaxLevel = eqMaxLevel,
            eqLevels = eqLevels,
            getCenterFreq = { band -> equalizerManager.getCenterFreq(band) },
            setBandLevel = { band, level -> equalizerManager.setBandLevel(band, level) },
            onReset = {
                equalizerManager.resetAllBands()
                for (i in 0 until eqBandCount) {
                    eqLevels[i] = 0f
                }
            },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showBoostDialog) {
        BoostDialog(
            preampLevel = preampLevel,
            loudnessBoostMb = loudnessBoostMb,
            onPreampChange = { newValue ->
                preampLevel = newValue
                player.volume = newValue
            },
            onBoostChange = { newValue ->
                loudnessBoostMb = newValue
                loudnessManager.setTargetGain(newValue.toInt())
            },
            onReset = {
                preampLevel = 1.0f
                loudnessBoostMb = 0f
                player.volume = 1.0f
                loudnessManager.setTargetGain(0)
            },
            onDismiss = { showBoostDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerActive = sleepTimerActive,
            onSetMinutes = { minutes ->
                sleepTimerRemainingMs = minutes * 60 * 1000L
                sleepTimerActive = true
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                sleepTimerActive = false
                sleepTimerRemainingMs = 0L
                showSleepTimerDialog = false
            },
            onDismiss = {
                showSleepTimerDialog = false
            }
        )
    }

    if (showReplayGainDialog) {
        ReplayGainDialog(
            replayGainMode = replayGainMode,
            replayGainScanning = replayGainScanning,
            onModeSelected = { mode ->
                replayGainMode = mode
                applyReplayGainForCurrentSong()
            },
            onScanLibrary = {
                scope.launch {
                    scanReplayGainPlaceholder()
                    applyReplayGainForCurrentSong()
                }
            },
            onDismiss = { showReplayGainDialog = false }
        )
    }

    if (showNowPlaying && currentSong != null) {
        NowPlayingDialog(
            song = currentSong,
            artwork = currentArtwork,
            isFavorite = favoriteSongIds.contains(currentSong.id),
            shuffleEnabled = shuffleEnabled,
            repeatLabel = repeatLabel(),
            currentPosition = currentPosition,
            duration = duration,
            isSeeking = isSeeking,
            seekPosition = seekPosition,
            sleepTimerActive = sleepTimerActive,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            onClose = { showNowPlaying = false },
            onToggleFavorite = { toggleFavorite(currentSong.id) },
            onCancelSleepTimer = {
                sleepTimerActive = false
                sleepTimerRemainingMs = 0L
            },
            onSeekChange = { newValue ->
                isSeeking = true
                seekPosition = newValue
            },
            onSeekFinished = {
                player.seekTo(seekPosition.toLong())
                currentPosition = seekPosition.toLong()
                isSeeking = false
            },
            onToggleShuffle = {
                shuffleEnabled = !shuffleEnabled
                player.shuffleModeEnabled = shuffleEnabled
            },
            onCycleRepeat = { cycleRepeatMode() },
            onPrev = {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                    player.play()
                    setupAudioEffects()
                    applyReplayGainForCurrentSong()
                }
            },
            onPlayPause = {
                if (player.isPlaying) player.pause() else player.play()
            },
            onNext = {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                    setupAudioEffects()
                    applyReplayGainForCurrentSong()
                }
            },
            onOpenQueue = {
                showQueueDialog = true
                queueVersion++
            },
            onOpenSleep = { showSleepTimerDialog = true },
            onOpenEqualizer = {
                setupAudioEffects()
                showEqualizerDialog = true
            },
            onOpenBoost = {
                setupAudioEffects()
                showBoostDialog = true
            },
            onOpenReplayGain = { showReplayGainDialog = true }
        )
    }

    if (showQueueDialog) {
        QueueDialog(
            player = player,
            queueVersion = queueVersion,
            onClose = { showQueueDialog = false },
            onPlayAt = { index ->
                player.seekToDefaultPosition(index)
                player.play()
                setupAudioEffects()
                applyReplayGainForCurrentSong()
                updateCurrentArtwork()
                queueVersion++
            },
            onMoveUp = { index ->
                if (index > 0) {
                    player.moveMediaItem(index, index - 1)
                    queueVersion++
                }
            },
            onMoveDown = { index ->
                if (index < player.mediaItemCount - 1) {
                    player.moveMediaItem(index, index + 1)
                    queueVersion++
                }
            },
            onRemoveAt = { index ->
                if (player.mediaItemCount > 1) {
                    player.removeMediaItem(index)
                    queueVersion++
                    updateCurrentArtwork()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (openedPlaylistId != -1L) {
                            "Playlist: $openedPlaylistName"
                        } else {
                            "Dj Drake"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            if (currentSong != null) {
                MiniPlayerBar(
                    title = currentSong.title,
                    artist = currentSong.artist,
                    artwork = currentArtwork,
                    isPlaying = isPlaying,
                    onOpen = { showNowPlaying = true },
                    onPlayPause = {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            if (player.mediaItemCount > 0) {
                                if (player.currentMediaItemIndex == -1) {
                                    player.seekToDefaultPosition(0)
                                }
                                player.play()
                                setupAudioEffects()
                                applyReplayGainForCurrentSong()
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->

        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "This app needs music permission to show songs on your phone.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        permissionLauncher.launch(permission)
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {


                if (openedPlaylistId == -1L) {

                    // HEADER: search row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left filter icon (does nothing yet)
                            IconButton(onClick = { /* TODO: open filters later */ }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = "Filters"
                                )
                            }

                            // Center fake search bar – tap to go to Search tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedTab = 6 },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search"
                                    )
                                    Text(
                                        text = "Search songs, playlists, and artists",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Right mic icon (placeholder)
                            IconButton(onClick = { /* TODO: voice search */ }) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice search"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // FEATURE CARDS: Favourites / Playlists / Recent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureCard(
                                label = "Favourites",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = 7 }
                            )
                            FeatureCard(
                                label = "Playlists",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTab = 5 }
                            )
                            FeatureCard(
                                label = "Recent",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    // TODO: implement recent – for now just stay on Songs
                                    selectedTab = 0
                                }
                            )
                        }
                    }

                    // TABS under the header (keep your existing tabs)
                    ScrollableTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Songs") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Albums") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Artists") })
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Genres") })
                        Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Folders") })
                        Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Playlists") })
                        Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Search") })
                        Tab(selected = selectedTab == 7, onClick = { selectedTab = 7 }, text = { Text("Favorites") })
                    }

                    if (selectedTab == 0) {
                        // (leave everything from here onwards exactly as you already have it)
                        if (songs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No songs found on this device.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = songs,
                                    key = { _, song -> song.id }
                                ) { index, song ->
                                    SongRow(
                                        song = song,
                                        isFavorite = favoriteSongIds.contains(song.id),
                                        onClick = {
                                            player.shuffleModeEnabled = shuffleEnabled
                                            player.repeatMode = repeatMode
                                            player.seekToDefaultPosition(index)
                                            player.play()
                                            setupAudioEffects()
                                            applyReplayGainForCurrentSong()
                                            currentArtwork = ArtworkRepository.loadEmbeddedArtwork(context, song.contentUri)
                                        },
                                        onToggleFavorite = {
                                            toggleFavorite(song.id)
                                        },
                                        onAddToPlaylist = {
                                            songToAdd = song
                                            showAddToPlaylistDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else if (selectedTab == 1) {
                        if (albums.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No albums found on this device.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(albums) { albumEntry ->
                                    val albumName = albumEntry.first
                                    val albumSongs = albumEntry.second
                                    val firstSong = albumSongs.firstOrNull()

                                    val albumArt = remember(albumName) {
                                        firstSong?.let {
                                            ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri)
                                        }
                                    }

                                    AlbumRow(
                                        albumName = albumName,
                                        songs = albumSongs,
                                        artwork = albumArt,
                                        onClick = {
                                            if (albumSongs.isNotEmpty()) {
                                                player.setMediaItems(albumSongs.map { it.toMediaItem(context) })
                                                player.prepare()
                                                player.shuffleModeEnabled = shuffleEnabled
                                                player.repeatMode = repeatMode
                                                player.seekToDefaultPosition(0)
                                                player.play()
                                                setupAudioEffects()
                                                applyReplayGainForCurrentSong()

                                                songs.indexOfFirst { it.id == albumSongs[0].id }
                                                currentArtwork = firstSong?.let {
                                                    ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else if (selectedTab == 2) {
                        if (artists.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No artists found on this device.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(artists) { artistEntry ->
                                    val artistName = artistEntry.first
                                    val artistSongs = artistEntry.second
                                    val firstSong = artistSongs.firstOrNull()

                                    val artistArt = remember(artistName) {
                                        firstSong?.let {
                                            ArtworkRepository.loadEmbeddedArtwork(
                                                context,
                                                it.contentUri
                                            )
                                        }
                                    }

                                    val albumCount = artistSongs
                                        .map { it.album }
                                        .distinct()
                                        .size

                                    ArtistRow(
                                        artistName = artistName,
                                        songs = artistSongs,
                                        albumCount = albumCount,
                                        artwork = artistArt,
                                        onClick = {
                                            if (artistSongs.isNotEmpty()) {
                                                player.setMediaItems(artistSongs.map { it.toMediaItem(context) })
                                                player.prepare()
                                                player.shuffleModeEnabled = shuffleEnabled
                                                player.repeatMode = repeatMode
                                                player.seekToDefaultPosition(0)
                                                player.play()
                                                setupAudioEffects()
                                                applyReplayGainForCurrentSong()

                                                songs.indexOfFirst { it.id == artistSongs[0].id }
                                                currentArtwork = firstSong?.let {
                                                    ArtworkRepository.loadEmbeddedArtwork(
                                                        context,
                                                        it.contentUri
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }else if (selectedTab == 3) {
                        if (genres.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No genres found on this device.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(genres) { genreEntry ->
                                    val genreName = genreEntry.first
                                    val genreSongs = genreEntry.second
                                    val firstSong = genreSongs.firstOrNull()
                                    val genreArt = remember(genreName) {
                                        firstSong?.let { ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri) }
                                    }
                                    val artistCount = genreSongs.map { it.artist }.distinct().size

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (genreSongs.isNotEmpty()) {
                                                    player.setMediaItems(genreSongs.map { it.toMediaItem(context) })
                                                    player.prepare()
                                                    player.shuffleModeEnabled = shuffleEnabled
                                                    player.repeatMode = repeatMode
                                                    player.seekToDefaultPosition(0)
                                                    player.play()
                                                    setupAudioEffects()
                                                    applyReplayGainForCurrentSong()
                                                    songs.indexOfFirst { it.id == genreSongs[0].id }
                                                    currentArtwork = firstSong?.let {
                                                        ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (genreArt != null) {
                                            Image(
                                                bitmap = genreArt.asImageBitmap(),
                                                contentDescription = "Genre art",
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("♪")
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = genreName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${genreSongs.size} songs • $artistCount artists",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    HorizontalDivider()
                                }
                            }
                        }
                    }else if (selectedTab == 4) {
                        if (folders.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No folders found on this device.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(folders) { folderEntry ->
                                    val folderName = folderEntry.first
                                    val folderSongs = folderEntry.second
                                    val firstSong = folderSongs.firstOrNull()
                                    val folderArt = remember(folderName) {
                                        firstSong?.let { ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri) }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (folderSongs.isNotEmpty()) {
                                                    player.setMediaItems(folderSongs.map { it.toMediaItem(context) })
                                                    player.prepare()
                                                    player.shuffleModeEnabled = shuffleEnabled
                                                    player.repeatMode = repeatMode
                                                    player.seekToDefaultPosition(0)
                                                    player.play()
                                                    setupAudioEffects()
                                                    applyReplayGainForCurrentSong()
                                                    songs.indexOfFirst { it.id == folderSongs[0].id }
                                                    currentArtwork = firstSong?.let {
                                                        ArtworkRepository.loadEmbeddedArtwork(context, it.contentUri)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (folderArt != null) {
                                            Image(
                                                bitmap = folderArt.asImageBitmap(),
                                                contentDescription = "Folder art",
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("📁")
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folderName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${folderSongs.size} songs",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    HorizontalDivider()
                                }
                            }
                        }
                    } else if (selectedTab == 5) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = { showCreatePlaylistDialog = true }
                            ) {
                                Text("Create Playlist")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (playlists.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No playlists yet.")
                                }
                            } else {
                                LazyColumn {
                                    items(playlists) { playlist ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .clickable {
                                                    scope.launch {
                                                        val playlistSongs = db.playlistDao().getSongsForPlaylist(playlist.id)

                                                        selectedPlaylistSongs = playlistSongs
                                                        openedPlaylistId = playlist.id
                                                        openedPlaylistName = playlist.name

                                                        val mediaItems = playlistSongs.map { it.toMediaItem(context) }
                                                        player.setMediaItems(mediaItems)
                                                        player.prepare()
                                                        player.shuffleModeEnabled = shuffleEnabled
                                                        player.repeatMode = repeatMode

                                                        if (playlistSongs.isNotEmpty()) {
                                                            player.seekToDefaultPosition(0)
                                                            player.play()
                                                            setupAudioEffects()
                                                            applyReplayGainForCurrentSong()
                                                            updateCurrentArtwork()
                                                        }
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = playlist.name,
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    } else if (selectedTab == 6) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search songs, artists, albums") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            when {
                                searchQuery.isBlank() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Start typing to search your music library.")
                                    }
                                }

                                filteredSongs.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No matching songs found.")
                                    }
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(filteredSongs) { _, song ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val originalIndex = songs.indexOfFirst { it.id == song.id }
                                                        if (originalIndex != -1) {
                                                            player.shuffleModeEnabled = shuffleEnabled
                                                            player.repeatMode = repeatMode
                                                            player.seekToDefaultPosition(originalIndex)
                                                            player.play()
                                                            setupAudioEffects()
                                                            applyReplayGainForCurrentSong()
                                                            currentArtwork = ArtworkRepository.loadEmbeddedArtwork(
                                                                context,
                                                                song.contentUri
                                                            )
                                                        }
                                                    }
                                                    .padding(vertical = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                AsyncImage(
                                                    model = song.contentUri,
                                                    contentDescription = "Album art",
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    contentScale = ContentScale.Crop
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = song.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${song.artist} • ${song.album}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = formatDuration(song.duration),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                    Text(
                                                        text = if (favoriteSongIds.contains(song.id)) "♥" else "♡",
                                                        color = if (favoriteSongIds.contains(song.id)) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.clickable {
                                                            toggleFavorite(song.id)
                                                        }
                                                    )
                                                }
                                            }

                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 7) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (favoriteSongs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No favorite songs yet.")
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(favoriteSongs) { _, song ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val originalIndex = songs.indexOfFirst { it.id == song.id }
                                                    if (originalIndex != -1) {
                                                        player.setMediaItems(songs.map { it.toMediaItem(context) })
                                                        player.prepare()
                                                        player.shuffleModeEnabled = shuffleEnabled
                                                        player.repeatMode = repeatMode
                                                        player.seekToDefaultPosition(originalIndex)
                                                        player.play()
                                                        setupAudioEffects()
                                                        applyReplayGainForCurrentSong()
                                                        currentArtwork = ArtworkRepository.loadEmbeddedArtwork(
                                                            context,
                                                            song.contentUri
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = song.contentUri,
                                                contentDescription = "Album art",
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${song.artist} • ${song.album}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = formatDuration(song.duration),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            Text(
                                                text = "♥",
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    toggleFavorite(song.id)
                                                }
                                            )
                                        }

                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    openedPlaylistId = -1L
                                    selectedPlaylistSongs = emptyList()
                                }
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    setupAudioEffects()
                                }
                            ) {
                                Text("EQ")
                            }

                            Button(
                                onClick = {
                                    setupAudioEffects()
                                }
                            ) {
                                Text("Boost")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedPlaylistSongs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("This playlist has no songs.")
                            }
                        } else {
                            LazyColumn {
                                itemsIndexed(selectedPlaylistSongs) { index, song ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                player.seekToDefaultPosition(index)
                                                player.play()
                                                setupAudioEffects()
                                                applyReplayGainForCurrentSong()
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${song.artist} • ${formatDuration(song.duration)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
