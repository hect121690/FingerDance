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

        /*
        val zipFile = File(rutaZip)
        val zipInputStream = ZipInputStream(FileInputStream(zipFile))
        var zipEntry: ZipEntry?

        while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/" + zipEntry?.name!!.replace("-Update", ""))

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
        */
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