package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class UnzipSongs(
    private val context: Context,
    private val nombreChannel: String,
    private val textView: TextView
) {
    val finishActivity = MutableLiveData<Boolean>()

    suspend fun performUnzip(rutaZip: String) {
        withContext(Dispatchers.IO) {
            val zipFile = ZipFile(rutaZip)
            val destino = File(context.getExternalFilesDir(null),"FingerDance/Songs/Channels/")
            zipFile.extractAll(destino.absolutePath)
        }

        withContext(Dispatchers.Main) {
            val ls = LoadSongsKsf()
            listChannels.clear()
            listEfectsDisplay.clear()
            listChannels = ls.getChannels(context)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            textView.text = "Recarga de canales completada."

            val zipFolder = File(
                context.getExternalFilesDir(null),
                "FingerDance/Songs/Channels/$nombreChannel"
            )
            zipFolder.deleteRecursively()

            Handler(Looper.getMainLooper()).postDelayed({
                textView.isVisible = false
                context.startActivity(Intent(context, MainActivity::class.java))
                finishActivity.value = true
            }, 2000)
        }
    }
}
