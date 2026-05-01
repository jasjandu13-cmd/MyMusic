package com.example.mymusic

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReplayGainDao {
    @Query("SELECT * FROM replay_gain WHERE songId = :songId LIMIT 1")
    suspend fun getReplayGain(songId: Long): ReplayGainEntity?

    @Query("SELECT * FROM replay_gain")
    suspend fun getAllReplayGain(): List<ReplayGainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReplayGain(entity: ReplayGainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReplayGain(items: List<ReplayGainEntity>)

    @Query("DELETE FROM replay_gain")
    suspend fun clearAll()
}