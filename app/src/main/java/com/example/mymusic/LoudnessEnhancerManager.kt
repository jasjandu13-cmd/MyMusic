package com.example.mymusic

import android.media.audiofx.LoudnessEnhancer

class LoudnessEnhancerManager {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun setup(audioSessionId: Int) {
        release()

        if (audioSessionId == 0) return

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
                setTargetGain(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTargetGain(gainMb: Int) {
        try {
            loudnessEnhancer?.setTargetGain(gainMb)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        loudnessEnhancer = null
    }
}