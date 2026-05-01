@file:OptIn(
    ExperimentalMaterial3Api::class,
    UnstableApi::class
)
@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package com.example.mymusic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MusicPlayerApp()
                }
            }
        }
    }
}

private fun getAudioPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MusicPlayerApp() {
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
        while (true) {
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
            songs = MusicRepository.getSongs(context)
            MusicRepository.cacheSongsInDatabase()
            loadPlaylists()
            loadFavorites()
            loadReplayGain()

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
            onDismissRequest = { },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            scope.launch {
                                db.playlistDao().insertPlaylist(PlaylistEntity(name = playlistName.trim()))
                                playlistName = ""
                                loadPlaylists()
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
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
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        equalizerManager.resetAllBands()
                        for (i in 0 until eqBandCount) {
                            eqLevels[i] = 0f
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            title = { Text("Equalizer") },
            text = {
                if (eqBandCount == 0) {
                    Text("Equalizer not available on this device.")
                } else {
                    LazyColumn {
                        items(eqBandCount) { index ->
                            val band = index.toShort()
                            val freqHz = equalizerManager.getCenterFreq(band) / 1000
                            val level = eqLevels[index] ?: 0f

                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("$freqHz Hz")
                                Slider(
                                    value = level,
                                    onValueChange = { newValue ->
                                        eqLevels[index] = newValue
                                        equalizerManager.setBandLevel(
                                            band,
                                            newValue.toInt().toShort()
                                        )
                                    },
                                    valueRange = eqMinLevel.toFloat()..eqMaxLevel.toFloat()
                                )
                                Text(
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%.2f dB",
                                        level / 100f
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showBoostDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        preampLevel = 1.0f
                        loudnessBoostMb = 0f
                        player.volume = 1.0f
                        loudnessManager.setTargetGain(0)
                    }
                ) {
                    Text("Reset")
                }
            },
            title = { Text("Sound Boost") },
            text = {
                Column {
                    Text(
                        text = "Preamp",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = preampLevel,
                        onValueChange = { newValue ->
                            preampLevel = newValue
                            player.volume = newValue
                        },
                        valueRange = 0.5f..1.5f
                    )
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%.2fx",
                            preampLevel
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Loudness Boost",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = loudnessBoostMb,
                        onValueChange = { newValue ->
                            loudnessBoostMb = newValue
                            loudnessManager.setTargetGain(newValue.toInt())
                        },
                        valueRange = 0f..1200f
                    )
                    Text(
                        text = "${loudnessBoostMb.toInt()} mB",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Use small boosts first. Higher boost can still distort on some songs or devices.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { }
                ) {
                    Text("Close")
                }
            },
            title = {
                Text("Sleep Timer")
            },
            text = {
                Column {
                    Button(
                        onClick = {
                            sleepTimerRemainingMs = 10 * 60 * 1000L
                            sleepTimerActive = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("10 minutes")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sleepTimerRemainingMs = 20 * 60 * 1000L
                            sleepTimerActive = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("20 minutes")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sleepTimerRemainingMs = 30 * 60 * 1000L
                            sleepTimerActive = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("30 minutes")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sleepTimerRemainingMs = 45 * 60 * 1000L
                            sleepTimerActive = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("45 minutes")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            sleepTimerRemainingMs = 60 * 60 * 1000L
                            sleepTimerActive = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("60 minutes")
                    }

                    if (sleepTimerActive) {
                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                sleepTimerActive = false
                                sleepTimerRemainingMs = 0L
                            }
                        ) {
                            Text("Cancel Current Timer")
                        }
                    }
                }
            }
        )
    }

    if (showReplayGainDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Close")
                }
            },
            title = { Text("ReplayGain") },
            text = {
                Column {
                    Button(
                        onClick = {
                            replayGainMode = ReplayGainMode.OFF
                            applyReplayGainForCurrentSong()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (replayGainMode == ReplayGainMode.OFF) "Mode: Off ✓" else "Mode: Off")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            replayGainMode = ReplayGainMode.TRACK
                            applyReplayGainForCurrentSong()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (replayGainMode == ReplayGainMode.TRACK) "Mode: Track ✓" else "Mode: Track")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            replayGainMode = ReplayGainMode.ALBUM
                            applyReplayGainForCurrentSong()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (replayGainMode == ReplayGainMode.ALBUM) "Mode: Album ✓" else "Mode: Album")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                scanReplayGainPlaceholder()
                                applyReplayGainForCurrentSong()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !replayGainScanning
                    ) {
                        Text(if (replayGainScanning) "Scanning..." else "Scan Library")
                    }
                }
            }
        )
    }

    if (showNowPlaying && currentSong != null) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { }) {
                            Text("Close")
                        }

                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.size(64.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (currentArtwork != null) {
                        Image(
                            bitmap = currentArtwork!!.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.42f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♪",
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                    }

                    if (sleepTimerActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sleep in ${formatSleepTime(sleepTimerRemainingMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                sleepTimerActive = false
                                sleepTimerRemainingMs = 0L
                            }
                        ) {
                            Text("Cancel Sleep Timer")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            toggleFavorite(currentSong.id)
                        }
                    ) {
                        Text(
                            if (favoriteSongIds.contains(currentSong.id)) {
                                "Remove from Favorites"
                            } else {
                                "Add to Favorites"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Slider(
                        value = if (duration > 0) {
                            if (isSeeking) seekPosition else currentPosition.toFloat()
                        } else {
                            0f
                        },
                        onValueChange = { newValue ->
                            isSeeking = true
                            seekPosition = newValue
                        },
                        onValueChangeFinished = {
                            player.seekTo(seekPosition.toLong())
                            currentPosition = seekPosition.toLong()
                            isSeeking = false
                        },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition))
                        Text(formatTime(duration))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                shuffleEnabled = !shuffleEnabled
                                player.shuffleModeEnabled = shuffleEnabled
                            }
                        ) {
                            Text(if (shuffleEnabled) "Shuffle On" else "Shuffle Off")
                        }

                        Button(
                            onClick = { cycleRepeatMode() }
                        ) {
                            Text(repeatLabel())
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (player.hasPreviousMediaItem()) {
                                    player.seekToPreviousMediaItem()
                                    player.play()
                                    setupAudioEffects()
                                    applyReplayGainForCurrentSong()
                                }
                            }
                        ) {
                            Text("Prev")
                        }

                        Button(
                            onClick = {
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
                        ) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }

                        Button(
                            onClick = {
                                if (player.hasNextMediaItem()) {
                                    player.seekToNextMediaItem()
                                    player.play()
                                    setupAudioEffects()
                                    applyReplayGainForCurrentSong()
                                }
                            }
                        ) {
                            Text("Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showQueueDialog = true
                                queueVersion++
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Queue")
                        }

                        Button(
                            onClick = {
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sleep")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                setupAudioEffects()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("EQ")
                        }

                        Button(
                            onClick = {
                                setupAudioEffects()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Boost")
                        }

                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RG")
                        }
                    }
                }
            }
        }
    }

    if (showQueueDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { }) {
                            Text("Close")
                        }

                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.size(64.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Current queue: ${player.mediaItemCount} tracks",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (player.mediaItemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Queue is empty.")
                        }
                    } else {
                        LazyColumn {
                            items(player.mediaItemCount) { index ->
                                queueVersion

                                val item = player.getMediaItemAt(index)
                                val title = item.mediaMetadata.title?.toString() ?: "Unknown title"
                                val artist = item.mediaMetadata.artist?.toString() ?: "Unknown artist"
                                val isCurrent = index == player.currentMediaItemIndex

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "Now Playing" else "Up Next",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    player.seekToDefaultPosition(index)
                                                    player.play()
                                                    setupAudioEffects()
                                                    applyReplayGainForCurrentSong()
                                                    updateCurrentArtwork()
                                                    queueVersion++
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Play")
                                            }

                                            Button(
                                                onClick = {
                                                    if (index > 0) {
                                                        player.moveMediaItem(index, index - 1)
                                                        queueVersion++
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Up")
                                            }

                                            Button(
                                                onClick = {
                                                    if (index < player.mediaItemCount - 1) {
                                                        player.moveMediaItem(index, index + 1)
                                                        queueVersion++
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Down")
                                            }

                                            Button(
                                                onClick = {
                                                    if (player.mediaItemCount > 1) {
                                                        player.removeMediaItem(index)
                                                        queueVersion++
                                                        updateCurrentArtwork()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (openedPlaylistId != -1L) {
                            "Playlist: $openedPlaylistName"
                        } else {
                            "Offline Music Player"
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
                    onOpen = { },
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
                                itemsIndexed(songs) { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                player.setMediaItems(songs.map { it.toMediaItem(context) })
                                                player.prepare()
                                                player.shuffleModeEnabled = shuffleEnabled
                                                player.repeatMode = repeatMode
                                                player.seekToDefaultPosition(index)
                                                player.play()
                                                setupAudioEffects()
                                                applyReplayGainForCurrentSong()
                                                currentArtwork = ArtworkRepository.loadEmbeddedArtwork(context, song.contentUri)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val songArt = remember(song.id) {
                                            ArtworkRepository.loadEmbeddedArtwork(context, song.contentUri)
                                        }

                                        if (songArt != null) {
                                            Image(
                                                bitmap = songArt.asImageBitmap(),
                                                contentDescription = "Album art",
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
                                                text = song.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${song.artist} • ${formatDuration(song.duration)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
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

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Add to playlist",
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    songToAdd = song
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
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
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (albumArt != null) {
                                            Image(
                                                bitmap = albumArt.asImageBitmap(),
                                                contentDescription = "Album art",
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
                                                text = albumName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${firstSong?.artist ?: "Unknown Artist"} • ${albumSongs.size} songs",
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (artistSongs.isNotEmpty()) {
                                                    player.setMediaItems(artistSongs.map {
                                                        it.toMediaItem(
                                                            context
                                                        )
                                                    })
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
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (artistArt != null) {
                                            Image(
                                                bitmap = artistArt.asImageBitmap(),
                                                contentDescription = "Artist art",
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
                                                text = artistName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${artistSongs.size} songs • $albumCount albums",
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
                                onClick = { }
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
                                                        playlist.name

                                                        val mediaItems = playlistSongs.map { it.toMediaItem(context) }

                                                        player.setMediaItems(mediaItems)
                                                        player.prepare()
                                                        player.shuffleModeEnabled = shuffleEnabled
                                                        player.repeatMode = repeatMode
                                                        setupAudioEffects()
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
                                                val songArt = remember(song.id) {
                                                    ArtworkRepository.loadEmbeddedArtwork(context, song.contentUri)
                                                }

                                                if (songArt != null) {
                                                    Image(
                                                        bitmap = songArt.asImageBitmap(),
                                                        contentDescription = "Album art",
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
                                            val songArt = remember(song.id) {
                                                ArtworkRepository.loadEmbeddedArtwork(context, song.contentUri)
                                            }

                                            if (songArt != null) {
                                                Image(
                                                    bitmap = songArt.asImageBitmap(),
                                                    contentDescription = "Album art",
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
private fun MiniPlayerBar(
    title: String,
    artist: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = "Album art",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪")
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPlayPause
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}
private fun buildSongMediaItem(
    context: android.content.Context,
    uri: Uri,
    title: String,
    artist: String,
    album: String
): MediaItem {
    val artworkBytes = ArtworkRepository.loadEmbeddedArtworkBytes(context, uri)

    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)

    artworkBytes?.let {
        metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
    }

    return MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}

private fun Song.toMediaItem(context: android.content.Context): MediaItem {
    return buildSongMediaItem(
        context = context,
        uri = contentUri,
        title = title,
        artist = artist,
        album = album
    )
}

@SuppressLint("UseKtx")
private fun StoredSong.toMediaItem(context: android.content.Context): MediaItem {
    return buildSongMediaItem(
        context = context,
        uri = contentUri.toUri(),
        title = title,
        artist = artist,
        album = album
    )
}
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun formatSleepTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun gainToLinear(gainDb: Float): Float {
    return 10.0.pow((gainDb / 20.0)).toFloat()
}

private fun albumKey(song: Song): String {
    return "${song.album.trim().lowercase(Locale.getDefault())}::${song.artist.trim().lowercase(Locale.getDefault())}"
}