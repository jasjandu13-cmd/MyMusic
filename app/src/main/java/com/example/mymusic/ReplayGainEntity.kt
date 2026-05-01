package com.example.mymusic

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replay_gain")
data class ReplayGainEntity(
    @PrimaryKey val songId: Long,
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null,
    val albumKey: String = ""
)