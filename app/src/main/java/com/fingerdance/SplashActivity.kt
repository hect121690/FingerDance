package com.fingerdance

import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

var tema: String = ""

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val FLOW_TAG = "SPLASH_FLOW"

        private const val MINIMUM_DISPLAY_TIME = 2_000L

        private const val DRIVE_CONNECT_TIMEOUT = 15_000
        private const val DRIVE_READ_TIMEOUT = 20_000

        /*
         * Máximo de solicitudes simultáneas hacia Google Drive.
         *
         * No conviene crear cientos de conexiones al mismo tiempo,
         * porque puede provocar bloqueos, errores 429, uso excesivo
         * de memoria y recolecciones de basura constantes.
         */
        private const val MAX_DRIVE_REQUESTS = 4
    }

    data class RemoteConfig(
        val flagActiveAllows: Boolean,
        val mpOn: Boolean,
        val numberUpdate: String,
        val paypalOn: Boolean,
        val rebootChannelsDrive: Boolean,
        val resetRegister: Boolean,
        val startOnline: Boolean,
        val timeHalfDouble: Long,
        val timeToPresiscion: Long,
        val timeAdjust: Long,
        val validFolders: List<String> = emptyList(),
        val version: String,
        val allowCheckValues: Boolean
    )

    private val driveSemaphore = Semaphore(MAX_DRIVE_REQUESTS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(FLOW_TAG, "1. SplashActivity iniciado")

        if (!isConnectedToInternet(this)) {
            Log.w(FLOW_TAG, "No hay conexión a internet")

            startActivity(
                Intent(
                    this,
                    NoInternetActivity::class.java
                )
            )

            finish()
            overridePendingTransition(0, 0)
            return
        }

        firebaseDatabase = FirebaseDatabase.getInstance()

        val webView = findViewById<WebView>(R.id.webViewSplash)

        webView.loadUrl(
            "file:///android_asset/splash.html"
        )

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            Log.d(FLOW_TAG, "2. Iniciando carga general")

            val initializationSuccessful = loadAllDataForApp()

            val elapsedTime =
                System.currentTimeMillis() - startTime

            val remainingSplashTime =
                (MINIMUM_DISPLAY_TIME - elapsedTime)
                    .coerceAtLeast(0L)

            if (remainingSplashTime > 0L) {
                delay(remainingSplashTime)
            }

            if (!initializationSuccessful) {
                Log.w(
                    FLOW_TAG,
                    "La inicialización terminó con errores. " +
                            "Se continuará hacia MainActivity."
                )
            }

            if (isFinishing || isDestroyed) {
                return@launch
            }

            Log.d(FLOW_TAG, "3. Abriendo MainActivity")

            startActivity(
                Intent(
                    this@SplashActivity,
                    MainActivity::class.java
                )
            )

            finish()
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Ejecuta toda la inicialización requerida por la aplicación.
     *
     * Devuelve true cuando la carga principal terminó correctamente.
     * Devuelve false si ocurrió un error no recuperable.
     */
    private suspend fun loadAllDataForApp(): Boolean {
        return try {
            Log.d(FLOW_TAG, "4. Cargando preferencias locales")

            getConfigToPreferences()

            Log.d(FLOW_TAG, "5. Consultando configuración Firebase")

            val config = fetchRemoteConfigSuspend()

            if (config != null) {
                setGlobalDataFromConfig(config)

                Log.d(
                    FLOW_TAG,
                    "6. Configuración Firebase cargada"
                )
            } else {
                Log.w(
                    FLOW_TAG,
                    "No se pudo obtener RemoteConfig. " +
                            "Se utilizarán valores locales."
                )
            }

            Log.d(FLOW_TAG, "7. Consultando validFolders")

            validFolders = getValidFoldersSuspend()

            Log.d(
                FLOW_TAG,
                "8. validFolders cargados: ${validFolders.size}"
            )

            Log.d(FLOW_TAG, "9. Iniciando datos de Google Drive")

            loadDriveDataSuspend(config)

            Log.d(FLOW_TAG, "10. Datos de Google Drive terminados")

            loadGradeResources()

            Log.d(FLOW_TAG, "11. Recursos de evaluación terminados")

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error durante la inicialización general",
                e
            )

            false
        }
    }

    private fun setGlobalDataFromConfig(
        config: RemoteConfig
    ) {
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
        allowCheckValues = config.allowCheckValues
    }

    /**
     * Espera realmente a que Firebase devuelva validFolders.
     *
     * La implementación anterior utilizaba callback, por lo que la
     * carga de Drive comenzaba antes de tener validFolders disponible.
     */
    private suspend fun getValidFoldersSuspend(): ArrayList<String> =
        suspendCancellableCoroutine { continuation ->

            val databaseRef = firebaseDatabase
                .getReference("version")
                .child("validFolders")

            val listener = object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {
                    val result = arrayListOf<String>()

                    for (folderSnapshot in snapshot.children) {
                        val folder =
                            folderSnapshot.value?.toString()

                        if (!folder.isNullOrBlank()) {
                            result.add(folder)
                        }
                    }

                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {
                    Log.e(
                        TAG,
                        "Error al leer validFolders",
                        error.toException()
                    )

                    if (continuation.isActive) {
                        continuation.resume(arrayListOf())
                    }
                }
            }

            databaseRef.addListenerForSingleValueEvent(
                listener
            )

            continuation.invokeOnCancellation {
                databaseRef.removeEventListener(listener)
            }
        }

    /**
     * Consulta los valores remotos de Firebase.
     */
    private suspend fun fetchRemoteConfigSuspend():
            RemoteConfig? =
        suspendCancellableCoroutine { continuation ->

            val databaseRef =
                firebaseDatabase.getReference("version")

            val listener = object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {
                    try {
                        val config = RemoteConfig(
                            flagActiveAllows =
                                snapshot
                                    .child("flagActiveAllows")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            mpOn =
                                snapshot
                                    .child("mpOn")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            numberUpdate =
                                snapshot
                                    .child("numberUpdate")
                                    .value
                                    ?.toString()
                                    .orEmpty(),

                            paypalOn =
                                snapshot
                                    .child("paypalOn")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            rebootChannelsDrive =
                                snapshot
                                    .child("rebootChannelsDrive")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            resetRegister =
                                snapshot
                                    .child("resetRegister")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            startOnline =
                                snapshot
                                    .child("startOnline")
                                    .getValue(Boolean::class.java)
                                    ?: false,

                            timeHalfDouble =
                                snapshot
                                    .child("timeHalfDouble")
                                    .value
                                    ?.toString()
                                    ?.toLongOrNull()
                                    ?: 0L,

                            timeToPresiscion =
                                snapshot
                                    .child("timeToPresiscion")
                                    .value
                                    ?.toString()
                                    ?.toLongOrNull()
                                    ?: 0L,

                            timeAdjust =
                                snapshot
                                    .child("time_adjust")
                                    .value
                                    ?.toString()
                                    ?.toLongOrNull()
                                    ?: 0L,

                            validFolders = emptyList(),

                            version =
                                snapshot
                                    .child("value")
                                    .value
                                    ?.toString()
                                    .orEmpty(),

                            allowCheckValues =
                                snapshot
                                    .child("allowCheckValues")
                                    .getValue(Boolean::class.java)
                                    ?: false
                        )

                        if (continuation.isActive) {
                            continuation.resume(config)
                        }
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error interpretando RemoteConfig",
                            e
                        )

                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {
                    Log.e(
                        TAG,
                        "Firebase canceló RemoteConfig",
                        error.toException()
                    )

                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            databaseRef.addListenerForSingleValueEvent(
                listener
            )

            continuation.invokeOnCancellation {
                databaseRef.removeEventListener(listener)
            }
        }

    /**
     * Carga en orden la información general de Google Drive.
     *
     * Cada función utiliza internamente Dispatchers.IO, por lo que
     * no es necesario envolver nuevamente todo el método en IO.
     */
    private suspend fun loadDriveDataSuspend(
        config: RemoteConfig?
    ) {
        try {
            Log.d(FLOW_TAG, "Drive: cargando archivos generales")

            getFilesDriveSuspend()

            Log.d(FLOW_TAG, "Drive: archivos generales terminados")
            Log.d(FLOW_TAG, "Drive: cargando temas")

            getThemesDriveSuspend()

            Log.d(FLOW_TAG, "Drive: temas terminados")
            Log.d(FLOW_TAG, "Drive: cargando canales y BGA")

            getChannelsWithBgaDriveSuspend(config)

            Log.d(FLOW_TAG, "Drive: canales y BGA terminados")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error cargando información de Google Drive",
                e
            )
        }
    }

    /**
     * Obtiene la lista principal de archivos de Drive.
     */
    private suspend fun getFilesDriveSuspend() {
        try {
            val encodedQuery = URLEncoder.encode(
                "'$FOLDER_ID' in parents and trashed=false",
                "UTF-8"
            )

            val url =
                "https://www.googleapis.com/drive/v3/files" +
                        "?q=$encodedQuery" +
                        "&fields=nextPageToken,files(id,name,mimeType,size)" +
                        "&pageSize=300" +
                        "&key=$API_KEY"

            val jsonResponse = executeDriveRequest(
                url = url,
                requestDescription = "archivos generales"
            ) ?: return

            val files =
                jsonResponse.optJSONArray("files")
                    ?: JSONArray()

            val result = arrayListOf<Pair<String, String>>()

            for (index in 0 until files.length()) {
                val file = files.optJSONObject(index)
                    ?: continue

                val name = file.optString("name")
                val id = file.optString("id")

                if (name.isNotBlank() && id.isNotBlank()) {
                    result.add(name to id)
                }
            }

            result.sortBy { it.first }

            listFilesDrive.clear()
            listFilesDrive.addAll(result)

            Log.d(
                FLOW_TAG,
                "Drive: ${result.size} archivos generales"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error obteniendo archivos generales",
                e
            )
        }
    }

    /**
     * Obtiene la lista de temas disponibles en Drive.
     */
    private suspend fun getThemesDriveSuspend() {
        try {
            val encodedQuery = URLEncoder.encode(
                "'$FOLDER_ID_THEMES' in parents and trashed=false",
                "UTF-8"
            )

            val url =
                "https://www.googleapis.com/drive/v3/files" +
                        "?q=$encodedQuery" +
                        "&fields=nextPageToken,files(id,name,mimeType,size)" +
                        "&pageSize=300" +
                        "&key=$API_KEY"

            val jsonResponse = executeDriveRequest(
                url = url,
                requestDescription = "temas"
            ) ?: return

            val files =
                jsonResponse.optJSONArray("files")
                    ?: JSONArray()

            val result = arrayListOf<Pair<String, String>>()

            for (index in 0 until files.length()) {
                val file = files.optJSONObject(index)
                    ?: continue

                val name = file.optString("name")
                val id = file.optString("id")

                if (name.isNotBlank() && id.isNotBlank()) {
                    result.add(name to id)
                }
            }

            result.sortBy { it.first }

            listThemesDrive.clear()
            listThemesDrive.addAll(result)

            Log.d(
                FLOW_TAG,
                "Drive: ${result.size} temas"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error obteniendo temas",
                e
            )
        }
    }

    /**
     * Carga canales desde caché o desde Google Drive.
     */
    private suspend fun getChannelsWithBgaDriveSuspend(
        config: RemoteConfig?
    ) {
        try {
            if (config?.rebootChannelsDrive == true) {
                Log.d(
                    FLOW_TAG,
                    "Eliminando caché de canales por configuración remota"
                )

                themes.edit()
                    .remove(KEY_CHANNELS_CACHE)
                    .apply()
            }

            val cachedChannels =
                getChannelsCacheSuspend()

            if (cachedChannels != null) {
                Log.d(
                    FLOW_TAG,
                    "Usando caché de canales: " +
                            "${cachedChannels.size} canales"
                )

                listChannelsDrive.clear()

                listChannelsDrive.addAll(
                    cachedChannels.sortedBy { it.name }
                )

                return
            }

            Log.d(
                FLOW_TAG,
                "No existe caché. Consultando canales en Drive"
            )

            val channels =
                getChannelsFromBgaDriveSuspend()
                    .sortedBy { it.name }

            listChannelsDrive.clear()
            listChannelsDrive.addAll(channels)

            if (channels.isNotEmpty()) {
                saveChannelsCacheSuspend(channels)
            }

            Log.d(
                FLOW_TAG,
                "Canales cargados desde Drive: ${channels.size}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error cargando canales con BGA",
                e
            )
        }
    }

    /**
     * Obtiene los canales, canciones y videos BGA.
     *
     * Los canales se procesan de forma secuencial para evitar crear
     * una cantidad excesiva de coroutines.
     *
     * Las canciones de cada canal se procesan concurrentemente,
     * pero executeDriveRequest limita las conexiones activas a 4.
     */
    private suspend fun getChannelsFromBgaDriveSuspend():
            List<MainActivity.ChannelsDrive> =
        coroutineScope {

            val channelsJson = listDriveChildrenSuspend(
                parentId = FOLDER_ID_CHANNELS_BGA,
                onlyFolders = true
            ) ?: return@coroutineScope emptyList()

            Log.d(
                FLOW_TAG,
                "Drive: ${channelsJson.length()} canales encontrados"
            )

            val channelsResult =
                arrayListOf<MainActivity.ChannelsDrive>()

            for (channelIndex in 0 until channelsJson.length()) {
                val channelObject =
                    channelsJson.optJSONObject(channelIndex)
                        ?: continue

                val channelId =
                    channelObject.optString("id")

                val channelName =
                    channelObject.optString("name")

                if (
                    channelId.isBlank() ||
                    channelName.isBlank()
                ) {
                    continue
                }

                Log.d(
                    FLOW_TAG,
                    "Canal ${channelIndex + 1}/" +
                            "${channelsJson.length()}: $channelName"
                )

                val songsJson = listDriveChildrenSuspend(
                    parentId = channelId,
                    onlyFolders = true
                ) ?: JSONArray()

                /*
                 * Extraemos primero id y nombre.
                 *
                 * Así evitamos acceder al mismo JSONArray desde
                 * múltiples coroutines simultáneamente.
                 */
                val songsMetadata =
                    ArrayList<Pair<String, String>>()

                for (songIndex in 0 until songsJson.length()) {
                    val songObject =
                        songsJson.optJSONObject(songIndex)
                            ?: continue

                    val songId =
                        songObject.optString("id")

                    val songName =
                        songObject.optString("name")

                    if (
                        songId.isNotBlank() &&
                        songName.isNotBlank()
                    ) {
                        songsMetadata.add(
                            songId to songName
                        )
                    }
                }

                Log.d(
                    FLOW_TAG,
                    "$channelName: ${songsMetadata.size} canciones"
                )

                val songsDeferred =
                    songsMetadata.map { metadata ->

                        val songId = metadata.first
                        val songName = metadata.second

                        async(Dispatchers.IO) {
                            loadSongDriveData(
                                songId = songId,
                                songName = songName
                            )
                        }
                    }

                val songs =
                    songsDeferred
                        .awaitAll()
                        .filterNotNull()
                        .sortedBy { it.name }

                channelsResult.add(
                    MainActivity.ChannelsDrive(
                        name = channelName,
                        id = channelId,
                        songs = ArrayList(songs)
                    )
                )

                Log.d(
                    FLOW_TAG,
                    "Canal terminado: $channelName"
                )
            }

            channelsResult
        }

    /**
     * Obtiene los videos de una canción.
     */
    private suspend fun loadSongDriveData(
        songId: String,
        songName: String
    ): MainActivity.SongsDrive? {
        return try {
            val videosJson = listDriveChildrenSuspend(
                parentId = songId,
                onlyFolders = false
            ) ?: return null

            val videos =
                arrayListOf<MainActivity.VideosDrive>()

            for (videoIndex in 0 until videosJson.length()) {
                val videoObject =
                    videosJson.optJSONObject(videoIndex)
                        ?: continue

                val mimeType =
                    videoObject.optString("mimeType")

                if (mimeType != "video/mp4") {
                    continue
                }

                val videoName =
                    videoObject.optString("name")

                val videoId =
                    videoObject.optString("id")

                /*
                 * Google Drive puede omitir size en algunos tipos
                 * de archivo. Por eso utilizamos optString.
                 */
                val videoSize =
                    videoObject.optString("size", "0")

                if (
                    videoName.isBlank() ||
                    videoId.isBlank()
                ) {
                    continue
                }

                videos.add(
                    MainActivity.VideosDrive(
                        name = videoName,
                        id = videoId,
                        size = videoSize
                    )
                )
            }

            MainActivity.SongsDrive(
                name = songName,
                id = songId,
                videos = ArrayList(
                    videos.sortedBy { it.name }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error cargando canción $songName",
                e
            )

            null
        }
    }

    /**
     * Consulta los elementos hijos de una carpeta de Drive.
     */
    private suspend fun listDriveChildrenSuspend(
        parentId: String,
        onlyFolders: Boolean = false
    ): JSONArray? {
        return try {
            val mimeFilter =
                if (onlyFolders) {
                    " and mimeType='application/vnd.google-apps.folder'"
                } else {
                    ""
                }

            val query = URLEncoder.encode(
                "'$parentId' in parents " +
                        "and trashed=false$mimeFilter",
                "UTF-8"
            )

            val url =
                "https://www.googleapis.com/drive/v3/files" +
                        "?q=$query" +
                        "&fields=nextPageToken,files(id,name,mimeType,size)" +
                        "&pageSize=300" +
                        "&key=$API_KEY"

            val response = executeDriveRequest(
                url = url,
                requestDescription = "carpeta $parentId"
            ) ?: return null

            response.optJSONArray("files")
                ?: JSONArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error consultando carpeta parentId=$parentId",
                e
            )

            null
        }
    }

    /**
     * Ejecuta una petición HTTP contra Google Drive.
     *
     * Características:
     * - Máximo cuatro peticiones simultáneas.
     * - Timeout de conexión.
     * - Timeout de lectura.
     * - Desconexión garantizada.
     * - Registro del cuerpo de error HTTP.
     */
    private suspend fun executeDriveRequest(
        url: String,
        requestDescription: String
    ): JSONObject? =
        driveSemaphore.withPermit {

            withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null

                try {
                    connection =
                        URL(url).openConnection()
                                as HttpURLConnection

                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout =
                            DRIVE_CONNECT_TIMEOUT
                        readTimeout =
                            DRIVE_READ_TIMEOUT
                        useCaches = false
                        doInput = true
                        instanceFollowRedirects = true

                        setRequestProperty(
                            "Accept",
                            "application/json"
                        )
                    }

                    val responseCode =
                        connection.responseCode

                    if (
                        responseCode in 200..299
                    ) {
                        val response =
                            connection
                                .inputStream
                                .bufferedReader()
                                .use { reader ->
                                    reader.readText()
                                }

                        JSONObject(response)
                    } else {
                        val errorResponse =
                            connection
                                .errorStream
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                .orEmpty()

                        Log.e(
                            TAG,
                            "Drive HTTP $responseCode en " +
                                    "$requestDescription. " +
                                    "Respuesta: $errorResponse"
                        )

                        null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error de red consultando " +
                                requestDescription,
                        e
                    )

                    null
                } finally {
                    connection?.disconnect()
                }
            }
        }

    /**
     * Lee la caché de canales.
     */
    private suspend fun getChannelsCacheSuspend():
            List<MainActivity.ChannelsDrive>? =
        withContext(Dispatchers.IO) {
            try {
                val json = themes.getString(
                    KEY_CHANNELS_CACHE,
                    null
                ) ?: return@withContext null

                if (json.isBlank()) {
                    return@withContext null
                }

                val type = object :
                    TypeToken<
                            List<MainActivity.ChannelsDrive>
                            >() {}.type

                Gson().fromJson<
                        List<MainActivity.ChannelsDrive>
                        >(json, type)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error leyendo caché de canales",
                    e
                )

                null
            }
        }

    /**
     * Guarda los canales en SharedPreferences.
     */
    private suspend fun saveChannelsCacheSuspend(
        channels: List<MainActivity.ChannelsDrive>
    ) {
        withContext(Dispatchers.IO) {
            try {
                val json = Gson().toJson(channels)

                themes.edit()
                    .putString(
                        KEY_CHANNELS_CACHE,
                        json
                    )
                    .apply()

                Log.d(
                    FLOW_TAG,
                    "Caché de canales guardada"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error guardando caché de canales",
                    e
                )
            }
        }
    }

    /**
     * Carga los recursos utilizados en la pantalla DanceGrade.
     */
    private suspend fun loadGradeResources() {
        val externalDirectory =
            getExternalFilesDir(null)

        if (externalDirectory == null) {
            Log.w(
                TAG,
                "getExternalFilesDir devolvió null"
            )

            return
        }

        val base =
            externalDirectory.absolutePath

        rutaGrades =
            "$base/FingerDance/Themes/" +
                    "$tema/GraphicsStatics/dance_grade"

        gradeDescription =
            "$base/FingerDance/Themes/" +
                    "$tema/GraphicsStatics/game_play/" +
                    "grade_description.png"

        gradeDescriptionAbrev =
            "$base/FingerDance/Themes/" +
                    "$tema/GraphicsStatics/game_play/" +
                    "grade_description_abrev.png"

        val gradesFile =
            File(rutaGrades)

        val descriptionFile =
            File(gradeDescription)

        val descriptionAbrevFile =
            File(gradeDescriptionAbrev)

        if (
            !gradesFile.exists() ||
            !descriptionFile.exists() ||
            !descriptionAbrevFile.exists()
        ) {
            Log.w(
                FLOW_TAG,
                "No existen todos los recursos de DanceGrade"
            )

            return
        }

        coroutineScope {
            val gradesDeferred =
                async(Dispatchers.IO) {
                    getGrades(rutaGrades)
                }

            val descriptionDeferred =
                async(Dispatchers.IO) {
                    getGradesDescription(
                        gradeDescription
                    )
                }

            val descriptionAbrevDeferred =
                async(Dispatchers.IO) {
                    getGradesDescription(
                        gradeDescriptionAbrev
                    )
                }

            AppResources.arrayGrades =
                gradesDeferred.await()

            AppResources.arrGradesDesc =
                descriptionDeferred.await()

            AppResources.arrGradesDescAbrev =
                descriptionAbrevDeferred.await()
        }
    }

    /**
     * Lee la configuración persistida de la aplicación.
     */
    private fun getConfigToPreferences() {
        try {
            tema =
                themes.getString(
                    "theme",
                    "default"
                ).orEmpty()

            skinSelected =
                themes.getString(
                    "skin",
                    ""
                ).orEmpty()

            speedSelected =
                themes.getString(
                    "speed",
                    ""
                ).orEmpty()

            showPadB =
                themes.getInt(
                    "showPadB",
                    0
                )

            hideImagesPadA =
                themes.getBoolean(
                    "hideImagesPadA",
                    false
                )

            skinPad =
                themes.getString(
                    "skinPad",
                    "default"
                ).orEmpty()

            alphaPadB =
                themes.getFloat(
                    "alphaPadB",
                    1f
                )

            versionUpdate =
                themes.getString(
                    "versionUpdate",
                    "0.0.0"
                ).orEmpty()

            valueOffset =
                themes.getLong(
                    "valueOffset",
                    0L
                )

            userName =
                themes.getString(
                    "userName",
                    ""
                ).orEmpty()

            isMidLine =
                themes.getBoolean(
                    "isMidLine",
                    false
                )

            isCounter =
                themes.getBoolean(
                    "isCounter",
                    false
                )

            breakSong =
                themes.getBoolean(
                    "breakSong",
                    true
                )

            typePadD =
                themes.getInt(
                    "typePadD",
                    0
                )

            numberUpdateLocal =
                themes.getString(
                    "numberUpdateLocal",
                    "0.0.0"
                ).orEmpty()

            isHorizontalMode =
                themes.getBoolean(
                    "isHorizontalMode",
                    false
                )

            playModeSingle =
                themes.getInt(
                    "playModeSingle",
                    0
                )

            playModeHalf =
                themes.getInt(
                    "playModeHalf",
                    0
                )

            if (tema.isBlank()) {
                tema = "default"
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error leyendo preferencias",
                e
            )

            if (tema.isBlank()) {
                tema = "default"
            }
        }
    }
}