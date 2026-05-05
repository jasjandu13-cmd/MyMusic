package com.example.mymusic

import android.content.Context

object AppSettings {
    private const val PREFS_NAME = "my_music_settings"

    private const val KEY_PREAMP = "preamp_level"
    private const val KEY_BOOST = "boost_level"
    private const val KEY_EQ_BAND_COUNT = "eq_band_count"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePreamp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_PREAMP, value).apply()
    }

    fun loadPreamp(context: Context): Float {
        return prefs(context).getFloat(KEY_PREAMP, 1.0f)
    }

    fun saveBoost(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_BOOST, value).apply()
    }

    fun loadBoost(context: Context): Float {
        return prefs(context).getFloat(KEY_BOOST, 0f)
    }

    fun saveEqBandLevel(context: Context, band: Int, value: Float) {
        prefs(context).edit().putFloat("eq_band_$band", value).apply()
    }

    fun loadEqBandLevel(context: Context, band: Int): Float {
        return prefs(context).getFloat("eq_band_$band", 0f)
    }

    fun saveEqBandCount(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_EQ_BAND_COUNT, value).apply()
    }

    fun loadEqBandCount(context: Context): Int {
        return prefs(context).getInt(KEY_EQ_BAND_COUNT, 0)
    }

    fun clearEq(context: Context, maxBands: Int = 20) {
        val editor = prefs(context).edit()
        for (i in 0 until maxBands) {
            editor.remove("eq_band_$i")
        }
        editor.remove(KEY_EQ_BAND_COUNT)
        editor.apply()
    }
}
