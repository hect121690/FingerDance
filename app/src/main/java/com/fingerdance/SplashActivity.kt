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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.IntBuffer

var tema : String = ""

class SplashActivity : AppCompatActivity() {

    // Data classes
    data class RemoteConfig(
        val flagActiveAllows: Boolean,
        val mpOn: Boolean,
        val numberUpdate: String,
        val paypalOn: Boolean,
        var rebootChannelsDrive: Boolean,
        val resetRegister: Boolean,
        val startOnline: Boolean,
        val timeHalfDouble: Long,
        val timeToPresiscion: Long,
        val timeAdjust: Long,
        var valiedFolders: List<String> = emptyList(),
        val version: String,
    )

    private val MINIMUM_DISPLAY_TIME = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (!isConnectedToInternet(this)) {
            startActivity(Intent(this, NoInternetActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
            return
        }

        //themes = getPreferences(MODE_PRIVATE)
        firebaseDatabase = FirebaseDatabase.getInstance()

        val webView = findViewById<WebView>(R.id.webViewSplash)
        webView.loadUrl("file:///android_asset/splash.html")

        lifecycleScope.launch {
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
            getConfigToPreferences()
            val config = fetchRemoteConfigSuspend()
            if(config != null){
                setGlobalDataFromConfig(config)
            }
            getValidFolders { folders ->
                validFolders = folders
            }
            loadDriveDataSuspend(config)

            val base = getExternalFilesDir(null)!!.absolutePath
            rutaGrades = "$base/FingerDance/Themes/$tema/GraphicsStatics/dance_grade"
            gradeDescription = "$base/FingerDance/Themes/$tema/GraphicsStatics/game_play/grade_description.png"
            gradeDescriptionAbrev = "$base/FingerDance/Themes/$tema/GraphicsStatics/game_play/grade_description_abrev.png"

            if (File(rutaGrades).exists() && File(gradeDescription).exists() && File(gradeDescriptionAbrev).exists()) {
                coroutineScope {
                    val gradesDeferred = async(Dispatchers.IO) { getGrades(rutaGrades) }
                    val descDeferred = async(Dispatchers.IO) { getGradesDescription(gradeDescription) }
                    val descAbrevDeferred = async(Dispatchers.IO) { getGradesDescription(gradeDescriptionAbrev) }
                    AppResources.arrayGrades = gradesDeferred.await()
                    AppResources.arrGradesDesc = descDeferred.await()
                    AppResources.arrGradesDescAbrev = descAbrevDeferred.await()
                }
            }

        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
        }
    }

    private fun setGlobalDataFromConfig(config: RemoteConfig) {
        flagActiveAllows = config.flagActiveAllows
        mpOn = config.mpOn
        numberUpdateFirebase = config.numberUpdate
        resetRegister = config.resetRegister
        paypalOn = config.paypalOn
        startOnline = config.startOnline
        TIME_ADJUST = config.timeAdjust
        timeToPresiscion = config.timeToPresiscion
        timeToPresiscionHD = config.timeHalfDouble
        versionUpdate = config.version
    }

    private fun getValidFolders(callback: (ArrayList<String>) -> Unit) {
        val databaseRef = firebaseDatabase.getReference("version").child("validFolders")
        val listResult = arrayListOf<String>()

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (folder in snapshot.children) {
                    listResult.add(folder.value.toString())
                }
                callback(listResult)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
    }

    private suspend fun fetchRemoteConfigSuspend(): RemoteConfig? =
        suspendCancellableCoroutine { continuation ->
            val ref = firebaseDatabase.getReference("version")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val config = RemoteConfig(
                            flagActiveAllows = snapshot.child("flagActiveAllows").getValue(Boolean::class.java) ?: false,
                            mpOn = snapshot.child("mpOn").getValue(Boolean::class.java) ?: false,
                            numberUpdate = snapshot.child("numberUpdate").getValue(String::class.java) ?: "",
                            paypalOn = snapshot.child("paypalOn").getValue(Boolean::class.java) ?: false,
                            rebootChannelsDrive = snapshot.child("rebootChannelsDrive").getValue(Boolean::class.java) ?: false,
                            resetRegister = snapshot.child("resetRegister").getValue(Boolean::class.java) ?: false,
                            startOnline = snapshot.child("startOnline").getValue(Boolean::class.java) ?: false,
                            timeHalfDouble = snapshot.child("timeHalfDouble").getValue(String::class.java)?.toLong() ?: 0L,
                            timeToPresiscion = snapshot.child("timeToPresiscion").getValue(String::class.java)?.toLong() ?: 0L,
                            timeAdjust = snapshot.child("time_adjust").getValue(String::class.java)?.toLong() ?: 0L,
                            version = snapshot.child("value").getValue(String::class.java) ?: "",
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
                themes.edit().putString(KEY_CHANNELS_CACHE, "").apply()
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

                val url = "https://www.googleapis.com/drive/v3/files?q=$query&fields=nextPageToken,files(id,name,mimeType,size)&pageSize=300&key=$API_KEY"
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
                val json = themes.getString(KEY_CHANNELS_CACHE, null)
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
                val json = Gson().toJson(channels)
                themes.edit().putString(KEY_CHANNELS_CACHE, json).apply()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
            }
        }
    }

    private fun getConfigToPreferences() {
        try {
            tema = themes.getString("theme", "default").toString()
            skinSelected = themes.getString("skin", "").toString()
            speedSelected = themes.getString("speed", "").toString()
            showPadB = themes.getInt("showPadB", 0)
            hideImagesPadA = themes.getBoolean("hideImagesPadA", false)
            skinPad = themes.getString("skinPad", "default").toString()
            alphaPadB = themes.getFloat("alphaPadB", 1f)
            versionUpdate = themes.getString("versionUpdate", "0.0.0").toString()
            valueOffset = themes.getLong("valueOffset", 0)
            userName = themes.getString("userName","").toString()
            isMidLine = themes.getBoolean("isMidLine",false)
            isCounter = themes.getBoolean("isCounter",false)
            breakSong = themes.getBoolean("breakSong",true)
            typePadD = themes.getInt("typePadD", 0)
            numberUpdateLocal = themes.getString("numberUpdateLocal", "0.0.0").toString()
            isHorizontalMode = themes.getBoolean("isHorizontalMode", false)

        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
        }
    }
}
