package com.example.mymusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player

class MusicViewModel : ViewModel() {

    // Basic UI state we can start with.
    // We will move more things here slowly instead of all at once.

    var selectedTab by mutableStateOf(0)
        private set

    var showNowPlaying by mutableStateOf(false)
        private set

    var showQueueDialog by mutableStateOf(false)
        private set

    var showCreatePlaylistDialog by mutableStateOf(false)
        private set

    var showAddToPlaylistDialog by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var openedPlaylistId by mutableStateOf(-1L)
        private set

    var openedPlaylistName by mutableStateOf("")
        private set

    fun selectTab(index: Int) {
        selectedTab = index
    }

    fun openNowPlaying() {
        showNowPlaying = true
    }

    fun closeNowPlaying() {
        showNowPlaying = false
    }

    fun openQueue() {
        showQueueDialog = true
    }

    fun closeQueue() {
        showQueueDialog = false
    }

    fun openCreatePlaylist() {
        showCreatePlaylistDialog = true
    }

    fun closeCreatePlaylist() {
        showCreatePlaylistDialog = false
    }

    fun openAddToPlaylist() {
        showAddToPlaylistDialog = true
    }

    fun closeAddToPlaylist() {
        showAddToPlaylistDialog = false
    }

    fun updateSearchQuery(value: String) {
        searchQuery = value
    }

    fun openPlaylist(id: Long, name: String) {
        openedPlaylistId = id
        openedPlaylistName = name
    }

    fun closePlaylist() {
        openedPlaylistId = -1L
        openedPlaylistName = ""
    }
}
