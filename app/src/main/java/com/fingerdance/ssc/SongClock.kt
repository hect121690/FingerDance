package com.fingerdance.ssc

import android.media.MediaPlayer
import android.media.MediaTimestamp
import android.os.Build
import androidx.annotation.RequiresApi

class SongClock(private val mediaPlayer: MediaPlayer) {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getPositionMs(): Double {
        if (!mediaPlayer.isPlaying) {
            return mediaPlayer.currentPosition.toDouble()
        }
        val timestamp = mediaPlayer.timestamp

        if (timestamp != null && timestamp != MediaTimestamp.TIMESTAMP_UNKNOWN) {
            val elapsedSystemNs = System.nanoTime() - timestamp.anchorSystemNanoTime
            val elapsedMediaUs = elapsedSystemNs / 1_000.0 * timestamp.mediaClockRate.toDouble()
            return (timestamp.anchorMediaTimeUs.toDouble() + elapsedMediaUs) / 1_000.0
        }
        return mediaPlayer.currentPosition.toDouble()
    }
}