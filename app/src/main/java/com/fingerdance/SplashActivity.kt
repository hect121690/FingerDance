package com.fingerdance

import android.content.Intent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

    // Variables de control para espera dinámica
    private var isLoadingComplete = false
    private var hasMinimumDisplayTime = false
    private val MINIMUM_DISPLAY_TIME = 2000L
    private val MAXIMUM_LOAD_TIME = 30000L
    private var startTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val webView = findViewById<WebView>(R.id.webViewSplash)
        webView.settings.apply {
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            databaseEnabled = true
        }
        webView.loadUrl("file:///android_asset/splash.html")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                loadAllDataForApp()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            hasMinimumDisplayTime = true
            checkAndNavigate()
        }, MINIMUM_DISPLAY_TIME)

        Handler(Looper.getMainLooper()).postDelayed({
            isLoadingComplete = true
            checkAndNavigate()
        }, MAXIMUM_LOAD_TIME)
    }

    private suspend fun loadAllDataForApp() = coroutineScope {
        try {
            val configDeferred = async(Dispatchers.IO) { fetchRemoteConfigSuspend() }
            val config = try { configDeferred.await() } catch (e: Exception) { null }
            //config!!.rebootChannelsDrive = true
            val driveDeferred = async(Dispatchers.IO) { loadDriveDataSuspend(config) }
            try { driveDeferred.await() } catch (e: Exception) { }

            if (config != null) {
                saveConfigToPreferences(config)
            }

            isLoadingComplete = true
            Handler(Looper.getMainLooper()).post { checkAndNavigate() }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error: ${e.message}")
            isLoadingComplete = true
            Handler(Looper.getMainLooper()).post { checkAndNavigate() }
        }
    }

    private fun checkAndNavigate() {
        if ((isLoadingComplete && hasMinimumDisplayTime) ||
            (System.currentTimeMillis() - startTime > MAXIMUM_LOAD_TIME)) {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private suspend fun fetchRemoteConfigSuspend(): RemoteConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseDatabase.getInstance()
                val databaseReference = db.getReference("version")
                var config: RemoteConfig? = null
                var finished = false

                databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        config = RemoteConfig(
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
                        finished = true
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error: ${error.message}")
                        finished = true
                    }
                })

                var count = 0
                while (!finished && count < 100) {
                    Thread.sleep(100)
                    count++
                }
                config
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error: ${e.message}")
                null
            }
        }
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
