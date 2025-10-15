package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class Unzip (private val context: Context) {

    suspend fun performUnzip(rutaZip: String, fileToUnzip: String, closeMain: Boolean) {

        withContext(Dispatchers.IO) {
            val zipFile = ZipFile(rutaZip)
            val destino = context.getExternalFilesDir(null)
            zipFile.extractAll(destino!!.absolutePath)
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if(closeMain) {
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            }
            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/$fileToUnzip")
            file.delete()
        }

        //zipInputStream.closeEntry()
        //zipInputStream.close()
    }
}