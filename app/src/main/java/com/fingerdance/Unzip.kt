package com.fingerdance

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class Unzip (private val context: Context) {
    private val BUFFER_SIZE = 1024

    fun performUnzip(rutaZip: String) {
        val zipFile = File(rutaZip)
        val zipInputStream = ZipInputStream(FileInputStream(zipFile))
        var zipEntry: ZipEntry?

        while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/" + zipEntry?.name)

            if (zipEntry?.isDirectory == true) {
                file.mkdirs()
            } else {
                val parent = File(file.parent)

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

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            val file = File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance.zip")
            file.delete()
        }

        zipInputStream.closeEntry()
        zipInputStream.close()
    }
}