package com.fingerdance

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build

class FingerDanceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        themes = getSharedPreferences("themes", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val audioManager = getSystemService(AudioManager::class.java)
            audioManager.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
        }
    }
}