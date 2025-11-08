package com.fingerdance

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ActivityOptionsBinding
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

var fileNameChannel = ""
class Options() : AppCompatActivity(), ItemClickListener {
    private lateinit var bgOptions : LinearLayout
    private lateinit var titleOptions : TextView

    private lateinit var btnGuardar : Button

    private lateinit var recyclerThemes : RecyclerView
    private lateinit var binding: ActivityOptionsBinding
    private lateinit var btnMoreThemes : Button
    private lateinit var recyclerFireBase: RecyclerView
    private lateinit var txProgress : TextView
    private lateinit var progressDownload: ProgressBar
    private lateinit var btnDescargar : Button
    private lateinit var btnCerrarDialog : Button
    private lateinit var txTitle : TextView
    private var filePath = ""
    private var fileNameTheme = ""

    private lateinit var btnTemas : Button
    private lateinit var btnCanciones : Button

    //private lateinit var btnCalibrity : Button

    private lateinit var layoutTemas : ConstraintLayout
    private lateinit var linearTextProgressChannel : LinearLayout
    private lateinit var layoutCanciones : ConstraintLayout
    private lateinit var layoutPads : ConstraintLayout

    //private lateinit var recyclerViewListChannels: RecyclerView
    private lateinit var scrollChannels: NestedScrollView
    private lateinit var downloadButtonChannel: Button
    private var selectedValueChannel: String? = null

    private lateinit var txProgressDownloadChannel : TextView
    private var isChannel = false

    private var nameNewChannel = ""
    private var descriptionNewChannel = ""
    private val idNewChannel = 100

    private val pickPreviewFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            saveFileToDestination(it)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_options)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        FirebaseApp.initializeApp(this)

        titleOptions = findViewById(R.id.titleOptions)
        binding = ActivityOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val layoutManager = LinearLayoutManager(this)
        recyclerThemes = findViewById(R.id.listThemes)
        recyclerThemes.layoutManager = layoutManager
        recyclerThemes = binding.listThemes
        btnMoreThemes = findViewById(R.id.btnMoreThemes)

        btnGuardar = findViewById(R.id.btnGuardar)

        btnTemas = findViewById(R.id.btnThemes)
        btnCanciones = findViewById(R.id.btnCanciones)

        //btnCalibrity = findViewById(R.id.button2)

        TextViewCompat.setCompoundDrawableTintList(btnTemas, ColorStateList.valueOf(Color.WHITE))
        TextViewCompat.setCompoundDrawableTintList(btnCanciones, ColorStateList.valueOf(Color.WHITE))

        layoutTemas = findViewById(R.id.layoutThemes)
        layoutCanciones = findViewById(R.id.layoutCanciones)
        layoutPads = findViewById(R.id.layoutPads)

        linearTextProgressChannel = findViewById(R.id.linearTextProgressChannel)

        val textSize = pxToSp((width/10).toFloat(), this)
        titleOptions.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        titleOptions.paintFlags = titleOptions.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        layoutTemas.visibility = View.GONE
        layoutCanciones.visibility = View.GONE
        layoutPads.visibility = View.GONE

        val btnGuardarPads = findViewById<Button>(R.id.btnGuardarPads)
        btnGuardarPads.visibility = View.INVISIBLE

        val open = ResourcesCompat.getDrawable(resources, android.R.drawable.arrow_down_float, null)
        val close = ResourcesCompat.getDrawable(resources, android.R.drawable.arrow_up_float, null)

        downloadButtonChannel = findViewById(R.id.download_button_channel)
        scrollChannels = findViewById(R.id.scrollChannels)
        //layoutCanciones.layoutParams.height = (height / 2)
        val arrowIndicator = findViewById<ImageView>(R.id.arrowIndicator)
        val txSlide = findViewById<TextView>(R.id.txSlide)
        scrollChannels.layoutParams.height = (height * 0.3).toInt()
        scrollChannels.layoutParams.width = (width * 0.7).toInt()

        scrollChannels.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                // Se está deslizando hacia abajo
                arrowIndicator.visibility = View.GONE
                txSlide.visibility = View.GONE
            }
        }

        txProgressDownloadChannel = findViewById(R.id.textViewDownloadChannel)
        txProgressDownloadChannel.layoutParams.width = (width / 10) * 9

        txProgressDownloadChannel.isVisible = false
        downloadButtonChannel.isEnabled = false

        val btnDeleteChannel = findViewById<Button>(R.id.deleteChannel)
        btnDeleteChannel.setOnClickListener {
            var nameChannelDelete = ""
            val layoutOptionsDelete = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val radioChannelsDelete = RadioGroup(this).apply {
                removeAllViews()
            }

            listChannels.forEach { channel ->
                val radioButton = RadioButton(this)
                radioButton.text = channel.nombre
                radioButton.id = View.generateViewId()
                radioChannelsDelete.addView(radioButton)
            }

            radioChannelsDelete.setOnCheckedChangeListener { group, checkedId ->
                val channelSelected = group.findViewById<RadioButton>(checkedId)
                nameChannelDelete = channelSelected.text.toString()
            }

            layoutOptionsDelete.addView(radioChannelsDelete)

            val dialogEliminar = AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Eliminar Canal")
                .setMessage("Selecciona el canal que deseas eliminar. Una vez eliminado, volverás a la pantalla principal")
                .setView(layoutOptionsDelete)
                .setPositiveButton("Eliminar") { _, _ ->
                    val dialogConfirmar = AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Confirmar")
                        .setMessage("¿Seguro que desea eliminar el canal $nameChannelDelete? Esta acción no se puede revertir.")
                        .setPositiveButton("Aceptar") { d, _ ->
                            deleteChannelFolder(nameChannelDelete)
                            db.deleteCanal(nameChannelDelete)
                            Toast.makeText(this, "El canal: $nameChannelDelete se ha eliminado", Toast.LENGTH_SHORT).show()
                            themes.edit().putString("allTunes", "").apply()
                            startActivity(Intent(this, MainActivity()::class.java))
                            this.finish()
                        }
                        .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                        .create()
                    dialogConfirmar.show()
                }
                .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                .create()

            dialogEliminar.show()
        }

        //deleteChannelFolder("")


        val radioChannelsDownload = RadioGroup(this).apply {
            removeAllViews()
        }

        listFilesDrive.forEach { channel ->
            val radioButton = RadioButton(this)
            radioButton.text = channel.first
            radioButton.id = View.generateViewId()
            radioButton.setTextColor(Color.WHITE)
            radioButton.textSize = pxToSp((height/50).toFloat(), this)
            radioButton.setPadding(0, 24, 0, 24)
            radioChannelsDownload.addView(radioButton)
        }

        scrollChannels.addView(radioChannelsDownload)

        radioChannelsDownload.setOnCheckedChangeListener { group, checkedId ->
            val channelSelected = group.findViewById<RadioButton>(checkedId)
            val itemList = listFilesDrive.find { it.first == channelSelected.text.toString()}
            selectedValueChannel = itemList!!.second
            fileNameChannel = itemList.first
            downloadButtonChannel.isEnabled = channelSelected.isChecked
            arrowIndicator.visibility = View.GONE
            txSlide.visibility = View.GONE
        }

        val switchImagePadA = findViewById<SwitchCompat>(R.id.showImagePadA)
        switchImagePadA.visibility = View.GONE
        switchImagePadA.layoutParams.width = width / 2

        val thumbColor = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(Color.GREEN, Color.RED))

        val trackColor = ColorStateList(
            arrayOf( intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(R.color.track_color_A, R.color.track_color_B))

        switchImagePadA.thumbTintList = thumbColor
        switchImagePadA.trackTintList = trackColor

        val txPercentAlpha = findViewById<TextView>(R.id.txPercentAlpha)
        txPercentAlpha.visibility = View.GONE
        val initAlpha = alphaPadB * 100
        txPercentAlpha.text = "Opacidad del pad B: ${initAlpha.toInt()}%"
        val seekBarAlphaPadB = findViewById<SeekBar>(R.id.seekBarAlphaPadB)
        seekBarAlphaPadB.layoutParams.width = width / 2
        seekBarAlphaPadB.progress = 100
        seekBarAlphaPadB.visibility = View.GONE
        seekBarAlphaPadB.progress = initAlpha.toInt()

        seekBarAlphaPadB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = progress / 100f
                txPercentAlpha.text = "Opacidad del pad B: $progress%"
                alphaPadB = String.format("%.2f", alpha).toFloat()
                themes.edit().putFloat("alphaPadB", alphaPadB).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val recyclerPadsB = findViewById<RecyclerView>(R.id.recyclerPadsB)
        recyclerPadsB.layoutManager = LinearLayoutManager(this)
        recyclerPadsB.visibility = View.GONE
        recyclerPadsB.layoutParams.width = width / 2
        recyclerPadsB.layoutParams.height = height / 2
        val listPadsB = getlistPadsB()

        recyclerPadsB.adapter = ListPadsAdapter(listPadsB) { selectedItem ->
            skinPad = selectedItem.text
            themes.edit().putString("skinPad", skinPad).apply()
        }

        val recyclerPadsC = findViewById<RecyclerView>(R.id.recyclerPadsC)
        recyclerPadsC.layoutManager = LinearLayoutManager(this)
        recyclerPadsC.visibility = View.GONE
        recyclerPadsC.layoutParams.width = width / 2
        recyclerPadsC.layoutParams.height = height / 2
        val listPadsC = getlistPadsC()

        recyclerPadsC.adapter = ListPadsAdapter(listPadsC) { selectedItem ->
            skinPad = selectedItem.text
            themes.edit().putString("skinPad", skinPad).apply()
        }

        val radioGroupPads = findViewById<RadioGroup>(R.id.radioGroupPads)
        radioGroupPads.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbPadA -> {
                    switchImagePadA.visibility = View.VISIBLE
                    recyclerPadsC.visibility = View.GONE
                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    showPadB = 0
                    themes.edit().putInt("showPadB", showPadB).apply()
                }
                R.id.rbPadB -> {
                    switchImagePadA.visibility = View.GONE
                    recyclerPadsC.visibility = View.GONE
                    txPercentAlpha.visibility = View.VISIBLE
                    seekBarAlphaPadB.visibility = View.VISIBLE
                    recyclerPadsB.visibility = View.VISIBLE
                    showPadB = 1
                    themes.edit().putInt("showPadB", showPadB).apply()
                }
                R.id.rbPadC -> {
                    switchImagePadA.visibility = View.GONE
                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    recyclerPadsC.visibility = View.VISIBLE
                    showPadB = 2
                    themes.edit().putInt("showPadB", showPadB).apply()
                }
                R.id.rbPadD -> {
                    switchImagePadA.visibility = View.GONE
                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    recyclerPadsC.visibility = View.GONE
                    showPadB = 3
                    themes.edit().putInt("showPadB", showPadB).apply()
                }
            }
        }

        switchImagePadA.setOnCheckedChangeListener { _, isChecked ->
            hideImagesPadA = isChecked
            themes.edit().putBoolean("hideImagesPadA", hideImagesPadA).apply()
            if(isChecked){
                switchImagePadA.setTextColor(Color.GREEN)
            }else{
                switchImagePadA.setTextColor(Color.RED)
            }
        }

        if(hideImagesPadA){
            switchImagePadA.isChecked = hideImagesPadA
        }

        downloadButtonChannel.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
            builder.setTitle("Aviso")
            builder.setMessage("Se descargara el canal seleccionado. Se recomienda usar una conexión Wi-Fi \n")
            builder.setCancelable(false)
            builder.setPositiveButton("Aceptar") { dialog, which ->
                when {
                    isUsingWifi(this) -> {
                        downloadChannel()
                    }
                    isUsingMobileData(this) -> {
                        mostrarDialogoDatosMoviles()
                    }
                }
            }
            builder.setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }

        val btnPad = findViewById<Button>(R.id.txPad)

        btnTemas.setOnClickListener{
            if(layoutTemas.visibility == View.VISIBLE){
                layoutTemas.visibility = View.GONE
                btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }else{
                layoutTemas.visibility = View.VISIBLE
                layoutCanciones.visibility = View.GONE
                btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
                btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                btnPad.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }
        }

        btnCanciones.setOnLongClickListener{
            themes.edit().putString("allTunes", "").apply()
            Toast.makeText(this, "Canales reiniciados", Toast.LENGTH_SHORT).show()
            true
        }

        btnCanciones.setOnClickListener {
            if(!isUsingWifi(this) && !isUsingMobileData(this)){
                mostrarDialogoSinConexion()
            }else{
                if(layoutCanciones.visibility == View.VISIBLE){
                    layoutCanciones.visibility = View.GONE
                    btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                }else{
                    layoutTemas.visibility = View.GONE
                    layoutPads.visibility = View.GONE
                    layoutCanciones.visibility = View.VISIBLE
                    btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
                    btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                    btnPad.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                }
            }

        }

        btnPad.setOnClickListener {
            if(layoutPads.visibility == View.VISIBLE){
                layoutPads.visibility = View.GONE
                btnPad.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }else{
                layoutTemas.visibility = View.GONE
                layoutCanciones.visibility = View.GONE
                layoutPads.visibility = View.VISIBLE
                btnGuardarPads.visibility = View.VISIBLE
                btnPad.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
                btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }
        }

        btnGuardarPads.setOnClickListener {
            btnPad.performClick()
            Toast.makeText(this, "Configuracion guardada.", Toast.LENGTH_SHORT).show()
            btnGuardarPads.visibility = View.INVISIBLE
        }

        val btnBreakSong = findViewById<SwitchCompat>(R.id.btnBreakSong)
        btnBreakSong.layoutParams.width = (width / 10) * 8
        btnBreakSong.isChecked = breakSong
        btnBreakSong.thumbTintList = thumbColor
        btnBreakSong.trackTintList = trackColor

        btnBreakSong.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.TransparentDialog).apply {
                setTitle("Contador Select Song")
                setCancelable(false)
            }
            if(!breakSong){
                dialog.setMessage(R.string.MessageBreakSongOn)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnBreakSong.isChecked = false
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnBreakSong.isChecked = true
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
            }else{
                dialog.setMessage(R.string.MessageBreakSongOff)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnBreakSong.isChecked = true
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnBreakSong.isChecked = false
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
            }
            dialog.show()
        }

        val btnNoCounter = findViewById<SwitchCompat>(R.id.btnNoCounter)
        btnNoCounter.layoutParams.width = (width / 10) * 8
        btnNoCounter.isChecked = isCounter
        btnNoCounter.thumbTintList = thumbColor
        btnNoCounter.trackTintList = trackColor

        btnNoCounter.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.TransparentDialog).apply {
                setTitle("Contador Select Song")
                setCancelable(false)
            }
            if(!isCounter){
                dialog.setMessage(R.string.MessageCounterOn)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnNoCounter.isChecked = false
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnNoCounter.isChecked = true
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
            }else{
                dialog.setMessage(R.string.MessageCounterOff)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnNoCounter.isChecked = true
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnNoCounter.isChecked = false
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
            }
            dialog.show()
        }

        val btnNoteMidLine = findViewById<SwitchCompat>(R.id.btnNoteMidLine)
        btnNoteMidLine.layoutParams.width = (width / 10) * 8
        btnNoteMidLine.isChecked = isMidLine
        btnNoteMidLine.thumbTintList = thumbColor
        btnNoteMidLine.trackTintList = trackColor

        btnNoteMidLine.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.TransparentDialog).apply {
                setTitle("Notas a media pantalla")
                setCancelable(false)
            }
            if(!isMidLine){
                dialog.setMessage(R.string.MessageMidLineOn)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnNoteMidLine.isChecked = false
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnNoteMidLine.isChecked = true
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
            }else{
                dialog.setMessage(R.string.MessageMidLineOff)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnNoteMidLine.isChecked = true
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnNoteMidLine.isChecked = false
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
            }

            dialog.show()
        }

        val btnOffline = findViewById<SwitchCompat>(R.id.btnOffline)
        btnOffline.layoutParams.width = (width / 10) * 8
        btnOffline.isChecked = isOffline

        btnOffline.thumbTintList = thumbColor
        btnOffline.trackTintList = trackColor
        btnOffline.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.TransparentDialog).apply {
                setTitle("Modo Offline")
                setCancelable(false)
            }
            if(!isOffline){
                dialog.setMessage(R.string.MessageOfflineOn)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                        btnOffline.isChecked = false
                        isOffline = btnOffline.isChecked
                        d.dismiss()
                    }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                        btnOffline.isChecked = true
                        isOffline = btnOffline.isChecked
                        d.dismiss()
                    }
            }else{
                dialog.setMessage(R.string.MessageOfflineOff)
                dialog.setNegativeButton("Cancelar"){ d, _ ->
                    btnOffline.isChecked = true
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar"){ d, _ ->
                    btnOffline.isChecked = false
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
            }
            dialog.show()
        }

        val constraintBG = findViewById<ConstraintLayout>(R.id.constraintBG)

        val txVersionNoteSkins = findViewById<TextView>(R.id.txVersionNoteSkin).apply {
            id = View.generateViewId()
            text = "Ultima versión de NoteSkins: $numberUpdate"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)

        }

        val txMyVersionNoteSkins =  findViewById<TextView>(R.id.txMyVersionNoteSkin).apply {
            id = View.generateViewId()
            text = "Tu versión de NoteSkins: $versionUpdate"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        val lbDescargando = TextView(this).apply {
            id = View.generateViewId()
            text = "Descargando:"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.WHITE)
            visibility = View.INVISIBLE
        }
        lbDescargando.textSize = 16f

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 15.dpToPx())
            visibility = View.INVISIBLE
        }

        val btnUpdateNoteskins =  findViewById<Button>(R.id.btnUptadeNoteSkin).apply {
            visibility = if (numberUpdate != versionUpdate) View.VISIBLE else View.INVISIBLE
        }

        btnUpdateNoteskins.setOnClickListener {
            btnUpdateNoteskins.isEnabled = false
            lbDescargando.visibility = View.VISIBLE
            lbDescargando.text = "Conectando..."

            CoroutineScope(Dispatchers.Main).launch {
                val downloadedFile = iniciarDescargaDrive { progress ->
                    runOnUiThread {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = progress
                        lbDescargando.text = "Descargando $progress%"

                        if (progress == 100) {
                            lbDescargando.text = "Descarga finalizada, espere por favor..."
                            themes.edit().putString("versionUpdate", numberUpdate).apply()
                            themes.edit().putString("efects", "").apply()
                            versionUpdate = numberUpdate
                        }
                    }
                }
                if (downloadedFile != null) {
                    lifecycleScope.launch {
                        val unzip = Unzip(this@Options)
                        val rutaZip =
                            getExternalFilesDir("FingerDance.zip").toString() //Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance.zip"
                        unzip.performUnzip(rutaZip, "FingerDance.zip", true)
                    }
                } else {
                    Toast.makeText(this@Options, "Error en la descarga", Toast.LENGTH_LONG).show()
                }
            }
        }

        constraintBG.addView(lbDescargando)
        constraintBG.addView(progressBar)

        lbDescargando.x = width / 7f
        lbDescargando.y = (height / 2f)
        lbDescargando.layoutParams.width = ((width / 7f) * 5).toInt()

        progressBar.x = width / 7f
        progressBar.y = (height / 2f)  - ((width / 14f))
        progressBar.layoutParams.width = ((width / 7) * 5)

        /*
        btnCalibrity.setOnClickListener {
            val intent = Intent(this, CalibrationActivity::class.java)
            startActivity(intent)
        }
        */

        val dir = getExternalFilesDir("/FingerDance/Themes/")
        val listThemes = ArrayList<ThemeItem>()
        val listRutasThemes = mutableListOf<String>()

        if (dir != null){
            dir.walkTopDown().forEach { file ->
                if (file.name.equals("GraphicsStatics", ignoreCase = true) && file.isDirectory) {
                    val hasFiles = file.walkTopDown().any { it.isFile }
                    val totalSize = file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    if (hasFiles && totalSize > 10_000_000) {
                        val ruta: String = file.toString().replace("/GraphicsStatics", "", ignoreCase = true)
                        listRutasThemes.add(ruta)
                    }
                }
            }
            listRutasThemes.sortBy { it }
            var nombre = ""
            var rutaBanner = ""
            for (index in 0 until listRutasThemes.size) {
                nombre = listRutasThemes[index].removeRange(0, 74)
                rutaBanner = listRutasThemes[index] + "/logo_theme.png"
                val themes = ThemeItem(rutaBanner, nombre, false)
                listThemes.add(themes)
            }
        }

        recyclerThemes.adapter = ThemesAdapter(listThemes)

        bgOptions = findViewById(R.id.bgOptions)
        val bit = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/bg.jpg").toString())
        bgOptions.foreground = BitmapDrawable(bit)

        btnGuardar.setOnClickListener{
            val theme = ThemesAdapter.getSelectedItem()
            themes.edit().putString("theme", theme.text).apply()
            themes.edit().putString("allTunes", "").apply()
            themes.edit().putString("efects", "").apply()
            listChannels.clear()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            this.finish()
        }

        val btnCargarTema = findViewById<Button>(R.id.btnCargarTema)
        btnCargarTema.setOnClickListener {
            isChannel = false
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val uri = Uri.fromFile(downloads)
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
            startActivityForResult(intent, 1001)
        }

        btnMoreThemes.setOnClickListener{
            Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show()
            runOnUiThread {
                showCustomDialog()
            }

        }

        val btnCreateChannel = findViewById<Button>(R.id.createChannel)
        btnCreateChannel.setOnClickListener {
            showInputNameChannel()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fileZipName = if(isChannel) "Canal" else "Tema"

        val localDirectory = if(isChannel){
            File(this@Options.getExternalFilesDir(null), "FingerDance/Songs/Channels/")
        }else{
            File(getExternalFilesDir(null), "FingerDance/Themes/")
        }
        val localFile = if(isChannel){
            File(localDirectory, fileNameChannel)
        }else{
            val zipUri = data?.data ?: return
            fileNameTheme = contentResolver.query(zipUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: ""
            File(localDirectory, fileNameTheme)
        }
        localDirectory.mkdirs()
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val zipUri = data?.data ?: return
            txProgressDownloadChannel.text = "Procesando paquete..."
            lifecycleScope.launch {
                try {
                    contentResolver.openInputStream(zipUri)?.use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if(isChannel){
                        lifecycleScope.launch {
                            val unzipSongs = UnzipSongs(this@Options, fileNameChannel, txProgressDownloadChannel)
                            unzipSongs.performUnzip(localFile.absolutePath)
                            unzipSongs.finishActivity.observe(this@Options) { shouldFinish ->
                                if (shouldFinish) finish()
                            }
                        }
                    }else{
                        themes.edit().putString("theme", fileNameTheme.replace(".zip", "", ignoreCase = true)).apply()
                        themes.edit().putString("efects", "").apply()
                        val unzipTheme = UnzipTheme(this@Options, fileNameTheme)
                        unzipTheme.performUnzip(localFile.absolutePath)
                    }
                    Toast.makeText(this@Options, "$fileZipName cargado correctamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@Options, "Error cargando $fileZipName: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun iniciarDescargaDrive(progressCallback: (Int) -> Unit): File? {
        val fallo = AlertDialog.Builder(this)
        fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/1D4sMohVuJ7aGOcSzNCijsdFGHUsAf-2R?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localFile = File(getExternalFilesDir(null), "FingerDance.zip")

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(localFile)
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytes = 0
                    val totalSize = connection.contentLength

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        val progress = (100.0 * totalBytes / totalSize).toInt()
                        progressCallback(progress)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    return@withContext localFile
                } else {
                    //println("Error en la descarga: Código ${connection.responseCode}")
                    fallo.show()
                    return@withContext null
                }
            } catch (e: Exception) {
                //println("Error descargando archivo: ${e.message}")
                fallo.show()
                return@withContext null
            }
        }

    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


    private fun showInputNameChannel() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val editTextChannel = EditText(this).apply {
            hint = "Nombre del canal"
        }

        val editTextDescription = EditText(this).apply {
            hint = "Descripción del canal"
        }

        layout.addView(editTextChannel)
        layout.addView(editTextDescription)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Crear canal")
            .setMessage("Por favor, ingresa el nombre y la descripción del canal")
            .setView(layout) // Asigna el contenedor como vista del diálogo
            .setPositiveButton("Aceptar") { _, _ ->
                if (editTextChannel.text.toString().isNotEmpty() && editTextDescription.text.toString().isNotEmpty()) {
                    nameNewChannel = idNewChannel.toString() + " - " + editTextChannel.text.toString().uppercase()
                    descriptionNewChannel = editTextDescription.text.toString()
                    if (getChannelExist()) {
                        deleteChannelFolder(nameNewChannel)
                    }
                    if (createPathNewChannel(this, nameNewChannel)) {
                        createTextIni(this)
                        showSelectIconChannel()
                    } else {
                        Toast.makeText(this, "Ocurrio un error al crear el canal, verifica los permisos de almacenamiento de la aplicación", Toast.LENGTH_SHORT).show()
                        showInputNameChannel()
                    }
                } else {
                    Toast.makeText(this, "Por favor, ingresa el nombre y la descripción del canal", Toast.LENGTH_SHORT).show()
                    showInputNameChannel()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
    private fun getChannelExist(): Boolean {
        val folderPath = this.getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel")
        return folderPath?.exists() ?: false
    }
    private fun showSelectIconChannel(){
        val dialog = AlertDialog.Builder(this)
            .setTitle("Crear canal")
            .setMessage("A continuación debera seleccionar el icono del canal, debe ser en formato PNG y medir 512x512 ó 1024x1024 px")
            .setPositiveButton("Aceptar") { _, _ ->
                pickPreviewFile.launch(arrayOf("image/png"))
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                deleteChannelFolder(nameNewChannel)
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    private fun createTextIni(context: Context): Boolean {
        val folderPath = context.getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel/info/")
        return if (folderPath != null && (folderPath.exists() || folderPath.mkdirs())) {
            val textFile = File(folderPath, "text.ini")
            try {
                textFile.writeText(descriptionNewChannel)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }
    private fun saveFileToDestination(uri: Uri?) {
        if (uri == null) {
            showSelectIconChannel()
            return
        }

        val destinationPath = getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel/")
        val destinationFile = File(destinationPath, "banner.png")

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (!((options.outWidth == 1024 && options.outHeight == 1024) || (options.outWidth == 512 && options.outHeight == 512))) {
                Toast.makeText(this, "La imagen debe ser de 512x512 o 1024x1024 píxeles", Toast.LENGTH_SHORT).show()
                showSelectIconChannel()
                return
            }
            val validatedInputStream: InputStream? = contentResolver.openInputStream(uri)
            validatedInputStream?.use { input ->
                val outputStream = FileOutputStream(destinationFile)
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            getSongsToCopy()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el archivo. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            showSelectIconChannel()
        }
    }
    private fun getSongsToCopy(){
        val dialog = AlertDialog.Builder(this)
            .setTitle("Crear canal")
            .setMessage("A continuación selecciona las carpeta donde se encuentras las canciones que quieres agregar al canal")
            .setPositiveButton("Aceptar") { _, _ ->
                openFolderPicker()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                deleteChannelFolder(nameNewChannel)
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val targetFolder = File(getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel")!!.path)
                copyAllFoldersWithContents(this, uri, targetFolder)
            }
        }
    }
    private fun copyAllFoldersWithContents(context: Context, sourceUri: Uri, targetBaseFolder: File) {
        val sourceFolder = DocumentFile.fromTreeUri(context, sourceUri)
        if (sourceFolder == null || !sourceFolder.isDirectory) {
            Toast.makeText(context, "No se puede acceder a la carpeta seleccionada", Toast.LENGTH_SHORT).show()
            return
        }

        if (!targetBaseFolder.exists()) targetBaseFolder.mkdirs()

        // Procesa todas las carpetas dentro de la carpeta seleccionada
        sourceFolder.listFiles().forEach { file ->
            if (file.isDirectory) {
                val targetSubFolder = File(targetBaseFolder, file.name!!)
                targetSubFolder.mkdirs()

                copyFolderContent(context, file, targetSubFolder)
                themes.edit().putString("allTunes", "").apply()
            }
        }
        Toast.makeText(this, "Se creó el canal correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun copyFolderContent(context: Context, sourceFolder: DocumentFile, targetFolder: File) {
        sourceFolder.listFiles().forEach { file ->
            if (file.isDirectory) {
                val newTargetSubFolder = File(targetFolder, file.name!!)
                newTargetSubFolder.mkdirs() // Crea subcarpeta en el destino
                copyFolderContent(context, file, newTargetSubFolder) // Llama recursivamente
            } else {
                val targetFile = File(targetFolder, file.name!!)
                context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }

    private fun createPathNewChannel(context: Context, nameFolder: String) : Boolean {
        var existFolder = true
        val folderPath = context.getExternalFilesDir("/FingerDance/Songs/Channels/$nameFolder/")
        if (folderPath != null && !folderPath.exists()) {
            existFolder = folderPath.mkdirs()
        }
        return existFolder
    }
    private fun deleteChannelFolder(nameChannel: String) {
        val folderPath = getExternalFilesDir("/FingerDance/Songs/Channels/$nameChannel/")
        folderPath?.deleteRecursively()
    }
    private fun getlistPadsB() : ArrayList<ThemeItem>{
        val dir = getExternalFilesDir("/FingerDance/PadsB/")
        val listThemes = ArrayList<ThemeItem>()
        dir?.walkTopDown()?.forEach {
            if(it.toString().endsWith(".png", true)){
                when {
                    it.isFile -> {
                        listThemes.add(ThemeItem(it.absolutePath, it.name.replace(".png", "", ignoreCase = true), false))
                    }
                }
            }
        }

        return  ArrayList(listThemes.sortedBy { it.text })
    }

    private fun getlistPadsC() : ArrayList<ThemeItem>{
        val dir = getExternalFilesDir("/FingerDance/PadsC/")
        val listThemes = ArrayList<ThemeItem>()
        dir?.walkTopDown()?.forEach {
            if(it.toString().endsWith("BG.png", true)){
                when {
                    it.isFile -> {
                        listThemes.add(ThemeItem(it.absolutePath, it.parentFile.name, false))
                    }
                }
            }
        }

        return  ArrayList(listThemes.sortedBy { it.text })
    }

    private fun downloadChannel(){
        downloadButtonChannel.isEnabled = false
        txProgressDownloadChannel.text = "Conectando..."
        //getDownloadChannel()
        getDownloadChannelDrive()
    }

    private fun getDownloadChannelDrive(){
        val localDirectory = File(getExternalFilesDir(null), "FingerDance/Songs/Channels/")
        localDirectory.mkdirs()
        val localFile = File(localDirectory, fileNameChannel)

        val progressBackground = txProgressDownloadChannel.background as LayerDrawable
        val progressLayer = progressBackground.findDrawableByLayerId(R.id.progress) as ClipDrawable
        linearTextProgressChannel.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // No hace nada
            }
        })
        CoroutineScope(Dispatchers.Main).launch {
            linearTextProgressChannel.visibility = View.VISIBLE
            txProgressDownloadChannel.isVisible = true

            val downloadedFile = downloadChannelFromDrive(selectedValueChannel!!, this@Options) { progress ->
                runOnUiThread {
                    txProgressDownloadChannel.text = "Descargando $progress%"
                    progressLayer.level = progress * 100
                    if(progress > 98){
                        txProgressDownloadChannel.text = "Iniciando descompresión..."
                        txProgressDownloadChannel.setTextColor(ContextCompat.getColor(this@Options, R.color.fondo_textview_vibrante))
                    }
                    if (progress == 100) {
                        txProgressDownloadChannel.text = "Recargando canales. Este proceso puede tomar varios minutos, no cierre esta pantalla."
                    }
                }
            }

            if (downloadedFile != null) {
                //Toast.makeText(this@Options, "Descarga completa: ${downloadedFile.absolutePath}", Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    val unzipSongs = UnzipSongs(this@Options, fileNameChannel, txProgressDownloadChannel)
                    unzipSongs.performUnzip(localFile.absolutePath)
                    unzipSongs.finishActivity.observe(this@Options) { shouldFinish ->
                        if (shouldFinish) finish()
                    }
                }
            } else {
                Toast.makeText(this@Options, "Error en la descarga", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadChannelFromDrive(fileId: String, context: Context, progressCallback: (Int) -> Unit): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localDirectory = File(context.getExternalFilesDir(null), "FingerDance/Songs/Channels/")
                    localDirectory.mkdirs()
                    val localFile = File(localDirectory, fileNameChannel)

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(localFile)
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytes = 0
                    val totalSize = connection.contentLength

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        val progress = (100.0 * totalBytes / totalSize).toInt()
                        progressCallback(progress)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    return@withContext localFile
                } else {
                    withContext(Dispatchers.Main) {
                        isChannel = true
                        showAlertFail(fileId)
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isChannel = true
                    showAlertFail(fileId)
                }
                return@withContext null
            }
        }
    }

    private fun mostrarDialogoSinConexion() {
        val noWifi = AlertDialog.Builder(this)
        noWifi.setMessage("No hay conexión a Internet. Reintentar?")
        noWifi.setPositiveButton("Reintentar") { dialog, which ->
            btnCanciones.performClick()
        }
        noWifi.setNegativeButton("Cerrar") { dialog, which ->
            dialog.dismiss()
        }
        noWifi.show()
    }

    private fun mostrarDialogoDatosMoviles() {
        val datosMoviles = AlertDialog.Builder(this, R.style.TransparentDialog)
        datosMoviles.setMessage("Está utilizando datos móviles. ¿Desea continuar?")
        datosMoviles.setPositiveButton("Aceptar") { dialog, which ->
            downloadChannel()
        }
        datosMoviles.setNegativeButton("Cancelar", null)
        datosMoviles.show()
    }

    private fun isUsingWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun isUsingMobileData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
    }

    private fun showCustomDialog() {
        val dialog = Dialog(this, R.style.TransparentDialog)
        dialog.setContentView(R.layout.dialog_more_themes)
        dialog.setCancelable(false)
        recyclerFireBase = dialog.findViewById(R.id.listThemesDropBox)
        txProgress = dialog.findViewById(R.id.txProgress)
        progressDownload = dialog.findViewById(R.id.progressMoreThemes)
        btnDescargar = dialog.findViewById(R.id.btnDescargar)
        btnCerrarDialog = dialog.findViewById(R.id.btnCerrarDialog)
        txTitle = dialog.findViewById(R.id.txTitle)
        val mitextoU = SpannableString("Selecciona el tema que deseas descargar.")
        mitextoU.setSpan(UnderlineSpan(), 0, mitextoU.length, 0)
        txTitle.setText(mitextoU)

        txProgress.visibility = View.INVISIBLE
        progressDownload.visibility = View.INVISIBLE
        btnDescargar.visibility = View.VISIBLE
        btnDescargar.isEnabled = false

        recyclerFireBase.layoutManager = LinearLayoutManager(this)

        val adapter = ThemesItemsAdapter(listThemesDrive, btnDescargar, this)
        recyclerFireBase.adapter = adapter
        dialog.show()

        btnDescargar.setOnClickListener {
            btnDescargar.visibility = View.INVISIBLE
            txProgress.visibility = View.VISIBLE
            progressDownload.visibility = View.VISIBLE
            txProgress.text = "Conetando..."
            btnCerrarDialog.isVisible = false
            val bgaPathSC = getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectChannel.mp4")!!.absolutePath
            if(File(bgaPathSC).isDirectory){
                File(bgaPathSC).delete()
            }
            val bgaPathSS = getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectSong.mp4")!!.absolutePath
            if(File(bgaPathSS).isDirectory){
                File(bgaPathSS).delete()
            }
            downloadThemeDrive()
        }
        btnCerrarDialog.setBackgroundColor(ContextCompat.getColor(this@Options, R.color.negative_red))
        btnCerrarDialog.setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onItemClick(item: Pair<String, String>) {
        filePath = item.second
        fileNameTheme = item.first
    }

    private fun downloadThemeDrive(){
        val localDirectory = File(getExternalFilesDir(null), "FingerDance/Themes/")
        localDirectory.mkdirs()
        val localFile = File(localDirectory, fileNameTheme)

        CoroutineScope(Dispatchers.Main).launch {
            val downloadedFile = downloadThemeFromDrive(filePath, this@Options) { progress ->
                runOnUiThread {
                    txProgress.text = "Descargando $progress%"
                    progressDownload.progress = progress
                    if(progress > 98){
                        txProgress.text = "Iniciando descompresión..."
                        txProgress.setTextColor(ContextCompat.getColor(this@Options, R.color.fondo_textview_vibrante))
                    }
                    if (progress == 100) {
                        txProgress.text = "Descarga completada"
                    }
                }
            }

            if (downloadedFile != null) {
                themes.edit().putString("theme", fileNameTheme.replace(".zip", "", ignoreCase = true)).apply()
                themes.edit().putString("efects", "").apply()
                val unzipTheme = UnzipTheme(this@Options, fileNameTheme)
                unzipTheme.performUnzip(localFile.absolutePath)
            } else {
                Toast.makeText(this@Options, "Error en la descarga", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadThemeFromDrive(fileId: String, context: Context, progressCallback: (Int) -> Unit): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localDirectory = File(context.getExternalFilesDir(null), "FingerDance/Themes/")
                    localDirectory.mkdirs()
                    val localFile = File(localDirectory, fileNameTheme)

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(localFile)
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytes = 0
                    val totalSize = connection.contentLength

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        val progress = (100.0 * totalBytes / totalSize).toInt()
                        progressCallback(progress)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    return@withContext localFile
                } else {
                    isChannel = false
                    withContext(Dispatchers.Main) {
                        showAlertFail(fileId)
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                isChannel = false
                withContext(Dispatchers.Main) {
                    showAlertFail(fileId)
                }
                return@withContext null
            }
        }
    }

    private fun showAlertFail(idDownload: String) {
        val fileDownload = if(isChannel) "canal" else "tema"
        val messageFail = "No se pudo realizar la descarga automatica, quieres descargar el $fileDownload manualmente? " +
                    "\n Si ya descargaste el $fileDownload, presiona el boton 'Cargar $fileDownload'"

        val urlManual = "https://drive.google.com/file/d/$idDownload/view?usp=drive_link"

        val alert = AlertDialog.Builder(this@Options, R.style.TransparentDialog)
            .setMessage(messageFail)
            .setPositiveButton("Descarga manual"){ d, _ ->
                val intent = Intent(Intent.ACTION_VIEW, urlManual.toUri()).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(null)
                }
                val chooser = Intent.createChooser(intent, "Abrir con navegador")
                startActivity(chooser)
                d.dismiss()
                if(isChannel){
                    txProgressDownloadChannel.isVisible = false
                    linearTextProgressChannel.visibility = View.GONE
                    downloadButtonChannel.isEnabled = true
                }else{
                    btnCerrarDialog.performClick()
                }
            }
            .setNegativeButton("Cargar $fileDownload"){ d, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val uri = Uri.fromFile(downloads)
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
                startActivityForResult(intent, 1001)
            }
            .show()

    }

    private class ThemesItemsAdapter(
        private val items: ArrayList<Pair<String, String>>,
        private val btnDescargar: Button,
        private val itemClickListener: ItemClickListener,
        ) : RecyclerView.Adapter<ThemesItemsAdapter.ViewHolder>() {
        private var selectedItemPosition = RecyclerView.NO_POSITION

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dropbox, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return items.size
        }
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textViewItem: TextView = itemView.findViewById(R.id.textViewItem)

            fun bind(item: Pair<String, String>) {
                textViewItem.text = item.first
                if (absoluteAdapterPosition == selectedItemPosition) {
                    textViewItem.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.progreso_textview_moderno))
                    btnDescargar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.progreso_textview_moderno))
                } else {
                    textViewItem.setBackgroundColor(Color.TRANSPARENT)
                }
                itemView.setOnClickListener {

                    val previousSelectedItemPosition = selectedItemPosition
                    selectedItemPosition = absoluteAdapterPosition

                    if (previousSelectedItemPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previousSelectedItemPosition)
                    }

                    notifyItemChanged(selectedItemPosition)

                    //val selectedItemText = item.second//textViewItem.text.toString()
                    itemClickListener.onItemClick(item)
                    btnDescargar.isEnabled = true
                }
            }
        }
    }

    private fun pxToSp(px: Float, context: Context): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    override fun onBackPressed() {
        //listFilesDrive.clear()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val decorView: View = window.decorView
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
}


