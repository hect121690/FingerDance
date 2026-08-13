package com.fingerdance

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.File
import androidx.core.graphics.drawable.toDrawable


var victoriesP1 = 0
var victoriesP2 = 0
var getSelectChannel = false


class SelectChannelOnline : AppCompatActivity() {

    private lateinit var lbNombreChannel: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var nav_izq: ImageView
    private lateinit var nav_der: ImageView
    private lateinit var nav_back_Izq: ImageView
    private lateinit var nav_back_der: ImageView
    private lateinit var imgAceptar: ImageView
    private lateinit var imgFloor: ImageView
    private lateinit var indicatorIzq: ImageView
    private lateinit var indicatorDer: ImageView
    private lateinit var imageCircle: ImageView
    private lateinit var bgaSelectChannel: VideoView
    private lateinit var recyclerChannels: RecyclerView
    private lateinit var txPlayer1: TextView
    private lateinit var txPlayer2: TextView
    private lateinit var linearWaitPlayer: LinearLayout
    private lateinit var txWaitForPlayer: TextView
    private val handlerSelectChannel = Handler(Looper.getMainLooper())
    private var position = 0
    private var animIndicator: Animation? = null
    private var roomListener: ValueEventListener? = null
    private var navigatingByRoom = false
    private var opponentDisconnectHandled = false


    // ============================================================
    // CONTROLES FÍSICOS
    // ============================================================

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) {
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_1 -> {
                    if (canControlSelection()) {
                        nav_izq.performClick()
                    }
                    true
                }

                KeyEvent.KEYCODE_3 -> {
                    if (canControlSelection()) {
                        nav_der.performClick()
                    }
                    true
                }
                KeyEvent.KEYCODE_5 -> {
                    if (canControlSelection()) {
                        imgAceptar.performClick()
                    }
                    true
                }
                KeyEvent.KEYCODE_7 -> {
                    nav_back_Izq.performClick()
                    true
                }
                KeyEvent.KEYCODE_9 -> {
                    nav_back_der.performClick()
                    true
                }
                else -> {
                    super.dispatchKeyEvent(event)
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_select_channel_online)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goMain(nav_back_der)
                }
            }
        )

        animIndicator = AnimationUtils.loadAnimation(this, R.anim.press_nav)

        setupViews()
        setupResources()
        setupPlayerInfo()
        setupListeners()
        updateRoomUi(activeSala)
    }


    // ============================================================
    // VIEWS
    // ============================================================

    private fun setupViews() {
        linearLayout = findViewById(R.id.background)
        nav_izq = findViewById(R.id.nav_izq)
        nav_der = findViewById(R.id.nav_der)
        nav_back_Izq = findViewById(R.id.nav_izq_gray)
        nav_back_der = findViewById(R.id.nav_der_gray)
        imgFloor = findViewById(R.id.floor)
        indicatorIzq = findViewById(R.id.indicatorIzq)
        indicatorDer = findViewById(R.id.indicatorDer)
        imgAceptar = findViewById(R.id.imgAceptar)
        imageCircle = findViewById(R.id.imageCircle)
        lbNombreChannel = findViewById(R.id.lbChannel)
        recyclerChannels = findViewById(R.id.recyclerChannels)
        bgaSelectChannel = findViewById(R.id.bgaSelectChannel)
        txPlayer1 = findViewById(R.id.txPlayer1)
        txPlayer2 = findViewById(R.id.txPlayer2)
        linearWaitPlayer = findViewById(R.id.linearWaitPlayer)

        txWaitForPlayer = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setShadowLayer(2.6f, 2.5f, 1.3f, android.graphics.Color.GREEN)
        }
        linearWaitPlayer.removeAllViews()
        linearWaitPlayer.addView(
            txWaitForPlayer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearWaitPlayer.setBackgroundColor("#CC000000".toColorInt())
        linearWaitPlayer.setOnClickListener { }
        linearWaitPlayer.bringToFront()
        val ancho = (width * 0.6).toInt()
        imageCircle.layoutParams.width = ancho
    }
    private fun setupResources() {
        AppResources.soundSelectChannel?.start()

        if (isFileExists(File(bgaPathSelectChannel))) {
            bgaSelectChannel.visibility = View.VISIBLE
            bgaSelectChannel.setVideoPath(bgaPathSelectChannel)
            bgaSelectChannel.setOnPreparedListener {
                it.setVolume(0f, 0f)
            }
            bgaSelectChannel.start()
            bgaSelectChannel.setOnCompletionListener {
                bgaSelectChannel.start()
            }
        } else {
            bgaSelectChannel.visibility = View.GONE
            linearLayout.foreground = AppResources.backgroundDrawable
        }

        indicatorIzq.setImageBitmap(AppResources.indicatorBitmap)
        indicatorIzq.rotation = 180f
        indicatorDer.setImageBitmap(AppResources.indicatorBitmap)

        imageCircle.setImageBitmap(AppResources.logoTheme)

        imgFloor.setImageBitmap(AppResources.bmFloor)
        imgAceptar.setImageBitmap(AppResources.bmAceptar)

        imgFloor.layoutParams.width = (width * 0.4).toInt()
        imgAceptar.layoutParams.width = (width * 0.2).toInt()

        val yDelta = width / 30
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 1.5f)).apply {
            duration = 400
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }

        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        nav_izq.setImageDrawable(animaNavs(AppResources.arrowNavIzq!!))
        nav_der.setImageDrawable(animaNavs(AppResources.arrowNavDer!!))
        nav_back_Izq.setImageDrawable(animaNavs(AppResources.arrowBackIzq!!))
        nav_back_der.setImageDrawable(animaNavs(AppResources.arrowBackDer!!))

        val adapter = CommandChannel(
            listChannelsOnline,
            (width * 0.6).toInt(),
            this
        )

        val layoutManager = object : LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        ) {
            override fun canScrollHorizontally(): Boolean = false
        }

        recyclerChannels.layoutManager = layoutManager
        recyclerChannels.isNestedScrollingEnabled = false
        recyclerChannels.adapter = adapter

        if (listChannelsOnline.isNotEmpty()) {
            val startPosition = Int.MAX_VALUE / 2
            val mod = startPosition % listChannelsOnline.size
            val finalStart = startPosition - mod

            recyclerChannels.scrollToPosition(finalStart)
            position = finalStart
            isFocusChannel(position)
        }

        val textSizeLabels = width / 20f

        lbNombreChannel.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX,
            textSizeLabels
        )

        txWaitForPlayer.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX,
            textSizeLabels / 2f
        )
    }


    // ============================================================
    // INFORMACIÓN DE JUGADORES
    // ============================================================

    private fun setupPlayerInfo() {
        txPlayer1.text = "Player 1\n${activeSala.jugador1.id}"
        txPlayer2.text = "Player 2\n${activeSala.jugador2.id}"
    }

    // ============================================================
    // LISTENERS UI
    // ============================================================

    private fun setupListeners() {
        nav_back_Izq.setOnClickListener {
            goMain(nav_back_Izq)
        }

        nav_back_der.setOnClickListener {
            goMain(nav_back_der)
        }


        // ========================================================
        // IZQUIERDA
        // ========================================================

        nav_izq.setOnClickListener {
            if (!canControlSelection()) {
                return@setOnClickListener
            }
            if (listChannelsOnline.isEmpty()) {
                return@setOnClickListener
            }
            AppResources.soundPool?.play(AppResources.channelMov, 1f,1f, 1, 0, 1f)
            nav_izq.startAnimation(animIndicator)
            indicatorIzq.startAnimation(animIndicator)
            iluminaIndicador(indicatorIzq)
            position--
            isFocusChannel(position)
        }


        // ========================================================
        // DERECHA
        // ========================================================

        nav_der.setOnClickListener {
            if (!canControlSelection()) {
                return@setOnClickListener
            }
            if (listChannelsOnline.isEmpty()) {
                return@setOnClickListener
            }

            AppResources.soundPool?.play(AppResources.channelMov, 1f, 1f, 1, 0, 1f)
            nav_der.startAnimation(animIndicator)
            indicatorDer.startAnimation(animIndicator)
            iluminaIndicador(indicatorDer)
            position++
            isFocusChannel(position)
        }


        // ========================================================
        // ACEPTAR CANAL
        // ========================================================

        imgAceptar.setOnClickListener {
            if (!canControlSelection()) {
                Toast.makeText(this, "Esperando selección de ${activeSala.turno}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (listChannelsOnline.isEmpty()) {
                return@setOnClickListener
            }
            imgAceptar.isEnabled = false
            AppResources.soundPool?.play(AppResources.pressStart, 1f, 1f, 1, 0, 1f)
            val adapter = recyclerChannels.adapter as CommandChannel
            val realPosition = adapter.getRealPosition(position)
            val channelSelected = listChannelsOnline[realPosition]

            if (channelSelected.listCanciones.isNotEmpty()) {
                AppResources.listSongsChannelKsf = channelSelected.listCanciones
                currentChannel = channelSelected.nombre
                navigateToSelectSong()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Aviso")
                    .setMessage("Este canal no contiene canciones.")
                    .setPositiveButton("OK") { d, _ ->
                        d.dismiss()
                    }
                    .show()

                imgAceptar.isEnabled = true
            }
        }

        imgFloor.setOnClickListener {
            imgAceptar.performClick()
        }
    }


    // ============================================================
    // FIREBASE
    // ============================================================

    private fun attachRoomListener() {
        if (roomListener != null) {
            return
        }
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sala = snapshot.getValue(Sala::class.java)

                if (sala == null) {
                    handleRoomClosed()
                    return
                }
                activeSala = sala
                setupPlayerInfo()

                if (sala.estado == RoomState.CLOSED.name) {
                    handleRoomClosed()
                    return
                }

                if (!isMyPlayerConnected(sala)) {
                    handleRoomClosed()
                    return
                }

                if (!isOpponentConnected(sala)) {
                    handleOpponentDisconnected()
                    return
                }

                opponentDisconnectHandled = false
                updateRoomUi(sala)
                checkRoomNavigation(sala)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SelectChannelOnline", "Error leyendo sala: ${error.message}")
            }
        }
        salaRef.addValueEventListener(roomListener!!)
    }

    private fun detachRoomListener() {
        val listener = roomListener ?: return
        salaRef.removeEventListener(listener)
        roomListener = null
    }


    // ============================================================
    // UI SEGÚN TURNO
    // ============================================================

    private fun updateRoomUi(sala: Sala) {
        val myTurn = sala.estado == RoomState.SELECTING.name && sala.turno == userName
        if (myTurn) {
            linearWaitPlayer.visibility = View.GONE
        } else {
            txWaitForPlayer.text =
                if (sala.estado == RoomState.SELECTING.name) {
                    "Esperando selección de\n${sala.turno}"
                } else {
                    "Sincronizando sala..."
                }
            linearWaitPlayer.visibility = View.VISIBLE
            linearWaitPlayer.bringToFront()
        }


        nav_izq.isEnabled = myTurn
        nav_der.isEnabled = myTurn
        imgAceptar.isEnabled = myTurn
        imgFloor.isEnabled = myTurn
    }


    // ============================================================
    // PLAYER QUE ESPERA
    // ============================================================
    private fun checkRoomNavigation(sala: Sala) {
        if (navigatingByRoom) {
            return
        }

        val amISelector = sala.turno == userName
        if (amISelector) {
            return
        }

        val selectorReady = isCurrentSelectorReady(sala)
        if (!selectorReady) {
            return
        }

        if (!hasSelectedSong(sala)) {
            return
        }
        navigatingByRoom = true

        val intent = Intent(this, SelectSongOnlineWait::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        overridePendingTransition(R.anim.anim_command_window_on, 0)
    }

    private fun isCurrentSelectorReady(sala: Sala): Boolean {
        return when (sala.turno) {
            sala.jugador1.id -> {
                sala.jugador1.listo
            }
            sala.jugador2.id -> {
                sala.jugador2.listo
            }
            else -> {
                false
            }
        }
    }

    private fun hasSelectedSong(sala: Sala): Boolean {
        val cancion = sala.cancion
        return cancion.nameSong.isNotBlank() &&
                cancion.rutaCancion.isNotBlank() &&
                cancion.rutaKsf.isNotBlank()
    }

    // ============================================================
    // PERMISOS DEL TURNO
    // ============================================================

    private fun canControlSelection(): Boolean {
        return activeSala.estado == RoomState.SELECTING.name &&
                activeSala.turno == userName &&
                isMyPlayerConnected(activeSala) &&
                isOpponentConnected(activeSala)
    }


    // ============================================================
    // CONEXIONES
    // ============================================================

    private fun isMyPlayerConnected(sala: Sala): Boolean {
        return if (isPlayer1) {
            sala.jugador1.id == userName && sala.jugador1.conectado
        } else {
            sala.jugador2.id == userName && sala.jugador2.conectado
        }
    }

    private fun isOpponentConnected(sala: Sala): Boolean {
        return if (isPlayer1) {
            sala.jugador2.id.isNotBlank() && sala.jugador2.conectado
        } else {
            sala.jugador1.id.isNotBlank() && sala.jugador1.conectado
        }
    }


    // ============================================================
    // RIVAL DESCONECTADO
    // ============================================================

    private fun handleOpponentDisconnected() {

        if (opponentDisconnectHandled || isFinishing) {
            return
        }
        opponentDisconnectHandled = true
        linearWaitPlayer.visibility = View.VISIBLE
        linearWaitPlayer.bringToFront()
        txWaitForPlayer.text = "El jugador ${if(isPlayer1) activeSala.jugador2.id else activeSala.jugador1.id} abandonó la sala"
        AlertDialog.Builder(this)
            .setTitle("Sala finalizada")
            .setMessage("El ${if(isPlayer1) activeSala.jugador2.id else activeSala.jugador1.id} se desconectó o abandonó la sala.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { d, _ ->
                d.dismiss()
                leaveRoom(goToMain = true)
            }
            .show()
    }


    // ============================================================
    // SALA CERRADA
    // ============================================================

    private fun handleRoomClosed() {
        if (isFinishing) {
            return
        }
        detachRoomListener()
        isOnline = false
        Toast.makeText(this, "La sala ya no está disponible", Toast.LENGTH_SHORT).show()
        finish()
    }


    // ============================================================
    // CHANNELS
    // ============================================================

    private fun isFocusChannel(position: Int) {
        if (listChannelsOnline.isEmpty()) {
            return
        }
        val adapter = recyclerChannels.adapter as CommandChannel
        val realPosition = adapter.getRealPosition(position)
        val item = listChannelsOnline[realPosition]
        recyclerChannels.scrollToPosition(position)
        channel = item.nombre
        lbNombreChannel.text = item.descripcion
        positionCurrentChannel = realPosition
    }


    // ============================================================
    // NAVIGATE SELECT SONG
    // ============================================================

    private fun navigateToSelectSong() {
        val intent = Intent(this, SelectSongOnline::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.anim_command_window_on, 0)
        imgAceptar.isEnabled = true
        AppResources.soundSelectChannel?.pause()
    }


    // ============================================================
    // SALIR
    // ============================================================

    private fun goMain(flecha: ImageView) {
        AppResources.soundPool?.play(AppResources.channelBack, 1f, 1f, 1,0, 1f)
        flecha.startAnimation(animIndicator)
        AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage("¿Deseas abandonar la sala?")
            .setPositiveButton("Aceptar") { d, _ ->
                d.dismiss()
                leaveRoom(goToMain = true)
            }
            .setNegativeButton("Cancelar") { d, _ ->
                d.dismiss()
            }
            .setCancelable(false)
            .show()
    }


    private fun leaveRoom(goToMain: Boolean) {
        detachRoomListener()
        if (isPlayer1) {
            salaRef.child("jugador1/conectado").onDisconnect().cancel()
            salaRef.removeValue()
        } else {
            salaRef.child("jugador2/conectado").onDisconnect().cancel()
            salaRef.child("jugador2").setValue(Jugador())
        }
        isOnline = false
        victoriesP1 = 0
        victoriesP2 = 0
        if (goToMain){
            AppResources.soundSelectChannel?.pause()
            finish()
        }
    }


    // ============================================================
    // ANIMACIONES
    // ============================================================
    private fun animaNavs(bitmap: Bitmap): AnimationDrawable {
        val spriteWidth = bitmap.width / 2
        val spriteHeight = bitmap.height / 2
        val animation = AnimationDrawable()

        for (r in 0 until 2) {
            for (c in 0 until 2) {
                val frame = Bitmap.createBitmap(
                    bitmap,
                    c * spriteWidth,
                    r * spriteHeight,
                    spriteWidth,
                    spriteHeight
                )

                animation.addFrame(frame.toDrawable(resources), 200)
            }
        }

        animation.isOneShot = false
        animation.start()

        return animation
    }

    private fun iluminaIndicador(imageView: ImageView?) {
        imageView ?: return
        ObjectAnimator.ofFloat(imageView, "alpha", 1f,0.3f, 1f).apply {
            duration = 500
        }.start()
    }

    // ============================================================
    // UTIL
    // ============================================================
    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    // ============================================================
    // LIFECYCLE FIREBASE
    // ============================================================

    override fun onStart() {
        super.onStart()
        attachRoomListener()
    }

    override fun onResume() {
        super.onResume()
        navigatingByRoom = false
        AppResources.soundSelectChannel?.start()
        try {
            bgaSelectChannel.resume()
            if (!bgaSelectChannel.isPlaying) {
                bgaSelectChannel.start()
            }

        } catch (_: Exception) { }

        updateRoomUi(activeSala)
    }

    override fun onPause() {
        super.onPause()
        AppResources.soundSelectChannel?.pause()
        if (::bgaSelectChannel.isInitialized && bgaSelectChannel.isPlaying) {
            bgaSelectChannel.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        detachRoomListener()
    }

    override fun onDestroy() {
        handlerSelectChannel.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ============================================================
    // FULLSCREEN
    // ============================================================

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}