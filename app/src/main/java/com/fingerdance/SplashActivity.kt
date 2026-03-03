package com.fingerdance

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.IntBuffer

var tema : String = ""

class SplashActivity : AppCompatActivity() {

    // Data classes
    data class RemoteConfig(
        val version: String,
        val flagActiveAllows: Boolean,
        val numberUpdate: String,
        val startOnline: Boolean,
        val resetRegister: Boolean,
        val paypalOn: Boolean,
        val mpOn: Boolean,
        val timeAdjust: Long,
        val timeHalfDouble: Int,
        val timeToPresiscion: Int,
        var rebootChannelsDrive: Boolean
    )

    private val MINIMUM_DISPLAY_TIME = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val preferences = getPreferences(MODE_PRIVATE)
        tema = preferences.getString("theme", "default").toString()

        val webView = findViewById<WebView>(R.id.webViewSplash)
        webView.loadUrl("file:///android_asset/splash.html")

        lifecycleScope.launch {

            // Ejecuta carga y tiempo mínimo en paralelo
            val loadJob = async { loadAllDataForApp() }
            val minTimeJob = async { delay(MINIMUM_DISPLAY_TIME) }

            loadJob.await()
            minTimeJob.await()

            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private suspend fun loadAllDataForApp() {
        try {
            val config = fetchRemoteConfigSuspend()
            loadDriveDataSuspend(config)

            if (config != null) {
                saveConfigToPreferences(config)
            }

            val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()
            val gradeDescription = "${rutaGrades.replace("dance_grade", "game_play")}/grade_description.png"
            val gradeDescriptionAbrev = "${rutaGrades.replace("dance_grade", "game_play")}/grade_description_abrev.png"

            coroutineScope {
                val gradesDeferred = async(Dispatchers.IO) { getGrades(rutaGrades) }
                val descDeferred = async(Dispatchers.IO) { getGradesDescription(gradeDescription) }
                val descAbrevDeferred = async(Dispatchers.IO) { getGradesDescription(gradeDescriptionAbrev) }

                AppResources.arrayGrades = gradesDeferred.await()
                AppResources.arrGradesDesc = descDeferred.await()
                AppResources.arrGradesDescAbrev = descAbrevDeferred.await()
            }


        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
        }
    }

    private fun getGrades(rutaGrades: String): ArrayList<Bitmap> {
        val bit = BitmapFactory.decodeFile("$rutaGrades/evaluation_grades 1x8.png")
        val cellWidth = bit.width / 2
        val cellHeight = bit.height / 8

        val gradesList = ArrayList<Bitmap>()

        for (r in 0 until 8) {
            for (c in 0 until 2) {
                val x = c * cellWidth
                val y = r * cellHeight
                val original = Bitmap.createBitmap(bit, x, y, cellWidth, cellHeight)
                val trimmed = trimTransparentEdges(original)
                gradesList.add(trimmed)
            }
        }

        return gradesList
    }

    private fun getGradesDescription(rutaGrades: String): ArrayList<Bitmap> {
        val bit = BitmapFactory.decodeFile(rutaGrades)
        val cellWidth = bit.width
        val cellHeight = bit.height / 8

        val gradesList = ArrayList<Bitmap>()

        for (r in 0 until 8) {
            val y = r * cellHeight
            val original = Bitmap.createBitmap(bit, 0, y, cellWidth, cellHeight)
            val trimmed = trimTransparentEdges(original)
            gradesList.add(trimmed)
        }

        return gradesList
    }

    private fun trimTransparentEdges(source: Bitmap): Bitmap {

        val width = source.width
        val height = source.height

        val pixels = IntArray(width * height)
        source.copyPixelsToBuffer(IntBuffer.wrap(pixels))

        var top = height
        var left = width
        var right = 0
        var bottom = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = pixels[y * width + x] ushr 24
                if (alpha != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        if (right <= left || bottom <= top) return source

        return Bitmap.createBitmap(
            source,
            left,
            top,
            right - left + 1,
            bottom - top + 1
        )
    }

    private suspend fun fetchRemoteConfigSuspend(): RemoteConfig? =
        suspendCancellableCoroutine { continuation ->

            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("version")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val config = RemoteConfig(
                            version = snapshot.child("value").getValue(String::class.java) ?: "",
                            flagActiveAllows = snapshot.child("flagActiveAllows").getValue(Boolean::class.java) ?: false,
                            numberUpdate = snapshot.child("numberUpdate").getValue(String::class.java) ?: "",
                            startOnline = snapshot.child("startOnline").getValue(Boolean::class.java) ?: false,
                            resetRegister = snapshot.child("resetRegister").getValue(Boolean::class.java) ?: false,
                            paypalOn = snapshot.child("paypalOn").getValue(Boolean::class.java) ?: false,
                            mpOn = snapshot.child("mpOn").getValue(Boolean::class.java) ?: false,
                            timeAdjust = snapshot.child("time_adjust").getValue(String::class.java)?.toLong() ?: 0L,
                            timeHalfDouble = snapshot.child("timeHalfDouble").getValue(String::class.java)?.toInt() ?: 0,
                            timeToPresiscion = snapshot.child("timeToPresiscion").getValue(Int::class.java) ?: 0,
                            rebootChannelsDrive = snapshot.child("rebootChannelsDrive").getValue(Boolean::class.java) ?: false
                        )

                        continuation.resume(config, null)

                    } catch (e: Exception) {
                        continuation.resume(null, null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(null, null)
                }
            })
        }

    private suspend fun loadDriveDataSuspend(config: RemoteConfig?) {
        return withContext(Dispatchers.IO) {
            try {
                getFilesDriveSuspend()
                getThemesDriveSuspend()
                getChannelsWithBgaDriveSuspend(config)
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
            }
        }
    }

    private suspend fun getFilesDriveSuspend() {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode("'$FOLDER_ID' in parents", "UTF-8")
                val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val jsonResponse = JSONObject(response)
                    val files = jsonResponse.getJSONArray("files")
                    listFilesDrive.clear()
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)
                        listFilesDrive.add(Pair(file.getString("name"), file.getString("id")))
                    }
                    listFilesDrive.sortBy { it.first }
                }
            } catch (e: Exception) {
                Log.d("Drive Files", "Error: ${e.message}")
            }
        }
    }

    private suspend fun getThemesDriveSuspend() {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode("'$FOLDER_ID_THEMES' in parents", "UTF-8")
                val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val jsonResponse = JSONObject(response)
                    val files = jsonResponse.getJSONArray("files")
                    listThemesDrive.clear()
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)
                        listThemesDrive.add(Pair(file.getString("name"), file.getString("id")))
                    }
                    listThemesDrive.sortBy { it.first }
                }
            } catch (e: Exception) {
                Log.d("Drive Themes", "Error: ${e.message}")
            }
        }
    }

    private suspend fun getChannelsWithBgaDriveSuspend(config: RemoteConfig?) = coroutineScope {
        try {
            if(config!!.rebootChannelsDrive){
                val preferences = getPreferences(MODE_PRIVATE)
                preferences.edit().putString(KEY_CHANNELS_CACHE, "").apply()
            }
            val cachedChannels = getChannelsCacheSuspend()
            if (cachedChannels != null) {
                listChannelsDrive.clear()
                listChannelsDrive.addAll(cachedChannels.sortedBy { it.name })
            } else {
                val channels = getChannelsWhithBgaDriveSuspend().sortedBy { it.name }
                listChannelsDrive.clear()
                listChannelsDrive.addAll(channels)
                saveChannelsCacheSuspend(channels)
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
        }
    }

    private suspend fun getChannelsWhithBgaDriveSuspend(): List<MainActivity.ChannelsDrive> = coroutineScope {
        val channelsJson = listDriveChildrenSuspend(FOLDER_ID_CHANNELS_BGA, onlyFolders = true)
            ?: return@coroutineScope emptyList()

        val channelDeferred = (0 until channelsJson.length()).map { i ->
            async(Dispatchers.IO) {
                val channelObj = channelsJson.getJSONObject(i)
                val channelId = channelObj.getString("id")
                val channelName = channelObj.getString("name")

                val songsJson = listDriveChildrenSuspend(channelId, onlyFolders = true)
                    ?: return@async null

                val songsDeferred = (0 until songsJson.length()).map { j ->
                    async(Dispatchers.IO) {
                        val songObj = songsJson.getJSONObject(j)
                        val songId = songObj.getString("id")
                        val songName = songObj.getString("name")

                        val videosJson = listDriveChildrenSuspend(songId, onlyFolders = false)
                            ?: return@async null

                        val videosList = mutableListOf<MainActivity.VideosDrive>()
                        for (k in 0 until videosJson.length()) {
                            val videoObj = videosJson.getJSONObject(k)
                            if (videoObj.getString("mimeType") == "video/mp4") {
                                videosList.add(
                                    MainActivity.VideosDrive(
                                        name = videoObj.getString("name"),
                                        id = videoObj.getString("id"),
                                        size = videoObj.getString("size")
                                    )
                                )
                            }
                        }

                        MainActivity.SongsDrive(
                            name = songName,
                            id = songId,
                            videos = ArrayList(videosList)
                        )
                    }
                }

                val songsList = songsDeferred.awaitAll().filterNotNull()
                MainActivity.ChannelsDrive(
                    name = channelName,
                    id = channelId,
                    songs = ArrayList(songsList)
                )
            }
        }

        channelDeferred.awaitAll().filterNotNull()
    }

    private suspend fun listDriveChildrenSuspend(parentId: String, onlyFolders: Boolean = false): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val mimeFilter = if (onlyFolders)
                    " and mimeType='application/vnd.google-apps.folder'"
                else ""

                val query = URLEncoder.encode(
                    "'$parentId' in parents and trashed=false$mimeFilter",
                    "UTF-8"
                )

                val url = "https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id,name,mimeType,size)&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    JSONObject(response).getJSONArray("files")
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("Drive", "Error: ${e.message}")
                null
            }
        }
    }

    private suspend fun getChannelsCacheSuspend(): List<MainActivity.ChannelsDrive>? {
        return withContext(Dispatchers.IO) {
            try {
                val preferences = getPreferences(MODE_PRIVATE)
                val json = preferences.getString(KEY_CHANNELS_CACHE, null)
                    ?: return@withContext null

                if (json.isBlank()) return@withContext null

                val type = object : TypeToken<List<MainActivity.ChannelsDrive>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
                null
            }
        }
    }

    private suspend fun saveChannelsCacheSuspend(channels: List<MainActivity.ChannelsDrive>) {
        return withContext(Dispatchers.IO) {
            try {
                val preferences = getPreferences(MODE_PRIVATE)
                val json = Gson().toJson(channels)
                preferences.edit().putString(KEY_CHANNELS_CACHE, json).apply()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
            }
        }
    }

    private fun saveConfigToPreferences(config: RemoteConfig) {
        try {
            val preferences = getPreferences(MODE_PRIVATE)
            preferences.edit().apply {
                putString("flagActiveAllows", config.flagActiveAllows.toString())
                putLong("TIME_ADJUST", config.timeAdjust)
                putInt("timeToPresiscion", config.timeToPresiscion)
                putInt("timeToPresiscionHD", config.timeHalfDouble)
                putBoolean("resetRegister", config.resetRegister)
                putBoolean("paypalOn", config.paypalOn)
                putBoolean("mpOn", config.mpOn)
                apply()
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
        }
    }
}
