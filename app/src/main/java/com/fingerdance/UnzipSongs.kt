package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import com.fingerdance.MainActivity
import com.fingerdance.ssc.LoadingSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class UnzipSongs(
    private val context: Context,
    private val nombreChannel: String,
    private val textView: TextView,
    private val message: String,
    private val deleteZip: Boolean
) {
    val finishActivity = MutableLiveData<Boolean>()

    suspend fun performUnzip(rutaZip: String) {
        withContext(Dispatchers.IO) {
            val zipFile = ZipFile(rutaZip)
            val destino = File(context.getExternalFilesDir(null),"FingerDance/Songs/Channels/")
            zipFile.extractAll(destino.absolutePath)
        }

        withContext(Dispatchers.Main) {
            listChannels.clear()
            listEfectsDisplay.clear()
            val listSongsKsf = LoadSongsKsf().getChannels(context)
            val listSongsSsc = LoadingSongs().getChannels(context)
            listChannels = ArrayList(listSongsKsf + listSongsSsc)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            textView.text = message

            val zipFolder = File(
                context.getExternalFilesDir(null),
                "FingerDance/Songs/Channels/$nombreChannel"
            )
            if(deleteZip) zipFolder.deleteRecursively()

            Handler(Looper.getMainLooper()).postDelayed({
                textView.isVisible = false
                context.startActivity(Intent(context, MainActivity::class.java))
                finishActivity.value = true
            }, 2000)
        }
    }
}
