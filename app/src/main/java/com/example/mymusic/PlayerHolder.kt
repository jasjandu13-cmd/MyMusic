package com.example.mymusic

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

object PlayerHolder {

    @Volatile
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    volume = 1.0f
                    player = this
                }
        }
    }

    fun release() {
        synchronized(this) {
            player?.release()
            player = null
        }
    }
}