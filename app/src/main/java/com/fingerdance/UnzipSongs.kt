package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import com.fingerdance.ssc.LoadingSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class UnzipSongs(
    private val context: Context,
    private val textView: TextView
) {

    val finishActivity = MutableLiveData<Boolean>()

    suspend fun performUnzip(
        rutaZip: String,
        deleteZip: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val zipFile = ZipFile(rutaZip)

            val destino = File(
                context.getExternalFilesDir(null),
                "FingerDance/Songs/Channels/"
            )

            if (!destino.exists()) {
                destino.mkdirs()
            }

            zipFile.extractAll(destino.absolutePath)

            if (deleteZip) {
                File(rutaZip).delete()
            }
        }
    }

    /**
     * Se llama una sola vez después de haber descomprimido
     * todos los canales seleccionados.
     */
    suspend fun reloadChannelsAndFinish(
        message: String = "Recarga de canales completada."
    ) {
        val updatedChannels = withContext(Dispatchers.IO) {
            listChannels.clear()
            listEfectsDisplay.clear()

            val listSongsKsf = LoadSongsKsf().getChannels(context)
            val listSongsSsc = LoadingSongs().getChannels(context)

            ArrayList(listSongsKsf + listSongsSsc)
        }

        withContext(Dispatchers.Main) {
            listChannels = updatedChannels

            themes.edit()
                .putString("allTunes", gson.toJson(listChannels))
                .apply()

            textView.text = message

            Handler(Looper.getMainLooper()).postDelayed({
                textView.isVisible = false

                context.startActivity(
                    Intent(context, MainActivity::class.java)
                )

                finishActivity.value = true
            }, 2000)
        }
    }
}