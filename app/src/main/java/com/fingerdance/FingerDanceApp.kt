package com.fingerdance

import android.app.Application

class FingerDanceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        themes = getSharedPreferences("themes", MODE_PRIVATE)
    }
}