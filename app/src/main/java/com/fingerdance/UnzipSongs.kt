package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class UnzipSongs (private val context: Context, private val nombreChannel: String, private val textView: TextView) {
    private val BUFFER_SIZE = 1024

    fun performUnzip(rutaZip: String) {

        val handler = Handler(Looper.getMainLooper())

        val zipFile = File(rutaZip)
        val zipInputStream = ZipInputStream(FileInputStream(zipFile))
        var zipEntry: ZipEntry?

        while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance/Songs/Channels/" + zipEntry?.name)

            if (zipEntry?.isDirectory == true) {
                file.mkdirs()
            } else {
                val parent = File(file.parent!!)

                if (!parent.exists()) {
                    parent.mkdirs()
                }

                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(BUFFER_SIZE)
                var len: Int

                while (zipInputStream.read(buffer).also { len = it } > 0) {
                    outputStream.write(buffer, 0, len)
                }

                outputStream.close()
            }
        }

        //val handler = Handler(Looper.getMainLooper())
        handler.post {
            //val intent = Intent(context, MainActivity::class.java)
            //context.startActivity(intent)

            themes.edit().putString("allTunes", "").apply()
            themes.edit().putString("efects", "").apply()
            val ls = LoadingSongs()
            val gson = Gson()
            listChannels.clear()
            listCommands.clear()
            listEfectsDisplay.clear()
            listChannels = ls.getChannels(context)
            listCommands = ls.getFilesCW(context)
            ls.loadImages(context)
            ls.loadSounds(context)

            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            themes.edit().putString("efects", gson.toJson(listCommands)).apply()

            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance//Songs/Channels/" + nombreChannel)
            file.delete()
        }

        zipInputStream.closeEntry()
        zipInputStream.close()
        handler.post {
            textView.text = "Racarga de canales completada."
            handler.postDelayed({
                textView.isVisible = false
                context.startActivity(Intent(context, MainActivity::class.java))
            }, 2000)
        }
    }
}