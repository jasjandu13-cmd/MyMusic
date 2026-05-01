package com.example.mymusic

import android.media.audiofx.Equalizer

class EqualizerManager {

    private var equalizer: Equalizer? = null

    fun setup(audioSessionId: Int) {
        release()

        if (audioSessionId == 0) return

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNumberOfBands(): Short {
        return try {
            equalizer?.numberOfBands ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getBandLevelRange(): ShortArray {
        return try {
            equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        } catch (_: Exception) {
            shortArrayOf(-1500, 1500)
        }
    }

    fun getCenterFreq(band: Short): Int {
        return try {
            equalizer?.getCenterFreq(band) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getBandLevel(band: Short): Short {
        return try {
            equalizer?.getBandLevel(band) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetAllBands() {
        try {
            val eq = equalizer ?: return
            for (band in 0 until eq.numberOfBands) {
                eq.setBandLevel(band.toShort(), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
    }
}