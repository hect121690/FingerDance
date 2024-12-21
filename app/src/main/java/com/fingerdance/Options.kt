package com.fingerdance

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ActivityOptionsBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class Options() : AppCompatActivity(), ItemClickListener {
    private lateinit var bgOptions : LinearLayout
    private lateinit var titleOptions : TextView
    private lateinit var recyclerThemes : RecyclerView
    private lateinit var binding: ActivityOptionsBinding
    private lateinit var btnGuardar : Button
    private lateinit var btnMoreThemes : Button
    private lateinit var recyclerFireBase: RecyclerView
    private lateinit var txProgress : TextView
    private lateinit var progressDownload: ProgressBar
    private lateinit var btnDescargar : Button
    private lateinit var txTitle : TextView
    private var filePath = ""

    private lateinit var btnTemas : Button
    private lateinit var btnCanciones : Button

    //private lateinit var btnCalibrity : Button

    private lateinit var layoutTemas : ConstraintLayout
    private lateinit var linearTextProgressChannel : LinearLayout
    private lateinit var layoutCanciones : ConstraintLayout
    private lateinit var layoutPads : ConstraintLayout

    private lateinit var items: MutableList<String>

    private lateinit var recyclerViewListChannels: RecyclerView
    private lateinit var downloadButtonChannel: Button
    private var selectedValueChannel: String? = null

    private lateinit var txProgressDownloadChannel : TextView

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
        btnGuardar = findViewById(R.id.btnGuardar)
        btnMoreThemes = findViewById(R.id.btnMoreThemes)

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

        //textSize = pxToSp((width/18).toFloat(), this)
        //titleTemas.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        layoutTemas.visibility = View.GONE
        layoutCanciones.visibility = View.GONE
        layoutPads.visibility = View.GONE

        val btnGuardarPads = findViewById<Button>(R.id.btnGuardarPads)
        btnGuardarPads.visibility = View.INVISIBLE

        val open = ResourcesCompat.getDrawable(resources, android.R.drawable.arrow_down_float, null)
        val close = ResourcesCompat.getDrawable(resources, android.R.drawable.arrow_up_float, null)

        items = getListOfThemesItems()

        downloadButtonChannel = findViewById(R.id.download_button_channel)
        recyclerViewListChannels = findViewById(R.id.recycler_view_list_channels)
        //layoutCanciones.layoutParams.height = (height / 2)
        recyclerViewListChannels.layoutParams.height = (width * 0.7).toInt()
        recyclerViewListChannels.layoutParams.width = (width * 0.7).toInt()

        txProgressDownloadChannel = findViewById(R.id.textViewDownloadChannel)
        txProgressDownloadChannel.layoutParams.width = (width / 10) * 9

        txProgressDownloadChannel.isVisible = false
        downloadButtonChannel.isEnabled = false
        val itemsChannels = getListChannels()
        val adapterChannels = ListChannelsAdapter(downloadButtonChannel, itemsChannels) { selectedItem ->
            selectedValueChannel = selectedItem
        }
        recyclerViewListChannels.layoutManager = LinearLayoutManager(this)
        recyclerViewListChannels.adapter = adapterChannels


        val switchImagePadA = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.showImagePadA)
        switchImagePadA.visibility = View.GONE
        switchImagePadA.layoutParams.width = width / 2

        val thumbColor = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)
        ),
                intArrayOf(Color.GREEN, Color.RED)
        )

        val trackColor = ColorStateList(
            arrayOf( intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(Color.parseColor("#80FF00"),Color.parseColor("#808080"))
        )

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
            builder.setMessage("Se descargara el canal seleccionado. Se recomienda usar una conexión Wi-Fi")
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
            builder.setNegativeButton("Cerrar") { dialog, which ->
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
            dir.walkTopDown().forEach {
                if(it.toString().endsWith("logo_theme.png", true)){
                    when {
                        it.isFile -> {
                            val ruta: String = it.toString().replace("/logo_theme.png", "", ignoreCase = true)
                            listRutasThemes.add(ruta)
                        }
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            this.finish()
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

    private var nameNewChannel = ""
    private var descriptionNewChannel = ""
    private val idNewChannel = 100
    private fun showInputNameChannel() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10) // Añade algo de margen alrededor
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
                        deleteChannelFolder()
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
            .setMessage("A continuación debera seleccionar el icono del canal, debe ser en formato PNG y medir 1024x1024 px")
            .setPositiveButton("Aceptar") { _, _ ->
                pickPreviewFile.launch(arrayOf("image/png"))
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                deleteChannelFolder()
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

            if (options.outWidth != 1024 || options.outHeight != 1024) {
                Toast.makeText(this, "La imagen debe ser de 1024x1024 píxeles", Toast.LENGTH_SHORT).show()
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
                deleteChannelFolder()
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
    fun createPathNewChannel(context: Context, nameFolder: String) : Boolean {
        var existFolder = true
        val folderPath = context.getExternalFilesDir("/FingerDance/Songs/Channels/$nameFolder/")
        if (folderPath != null && !folderPath.exists()) {
            existFolder = folderPath.mkdirs()
        }
        return existFolder
    }
    private fun deleteChannelFolder() {
        val folderPath = getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel/")
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
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        val fileRef = storageReference.child("Channels/" + selectedValueChannel)

        val localDirectory = File(getExternalFilesDir(null), "FingerDance/Songs/Channels/")
        localDirectory.mkdirs()
        val localFile = File(localDirectory, selectedValueChannel)

        val fallo = AlertDialog.Builder(this)
        fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")
        val progressBackground = txProgressDownloadChannel.background as LayerDrawable
        val progressLayer = progressBackground.findDrawableByLayerId(R.id.progress) as ClipDrawable

        fileRef.getFile(localFile).addOnSuccessListener {

        }.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            linearTextProgressChannel.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    // No hace nada
                }
            })
            txProgressDownloadChannel.isVisible = true
            txProgressDownloadChannel.text = "Descargando $progress%"
            progressLayer.level = progress * 100

            if(progress > 98){
                txProgressDownloadChannel.text = "Iniciando descompresión..."
                txProgressDownloadChannel.setTextColor(ContextCompat.getColor(this, R.color.fondo_textview_vibrante))
            }
            if (progress == 100) {
                txProgressDownloadChannel.text = "Recargando canales. Este proceso puede tomar varios minutos, no cierre esta pantalla."
                val unzipTheme = UnzipSongs(this, selectedValueChannel!!, txProgressDownloadChannel)
                unzipTheme.finishActivity.observe(this) { shouldFinish ->
                    if (shouldFinish) finish()
                }
                unzipTheme.performUnzip(localFile.absolutePath)
            }
        }.addOnFailureListener {
            fallo.show()
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

    fun isUsingWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    fun isUsingMobileData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_more_themes)

        recyclerFireBase = dialog.findViewById(R.id.listThemesDropBox)
        txProgress = dialog.findViewById(R.id.txProgress)
        progressDownload = dialog.findViewById(R.id.progressMoreThemes)
        btnDescargar = dialog.findViewById(R.id.btnDescargar)
        txTitle = dialog.findViewById(R.id.txTitle)
        val mitextoU = SpannableString("Selecciona el tema que deseas descargar.")
        mitextoU.setSpan(UnderlineSpan(), 0, mitextoU.length, 0)
        txTitle.setText(mitextoU)

        txProgress.visibility = View.INVISIBLE
        progressDownload.visibility = View.INVISIBLE
        btnDescargar.isEnabled = false

        recyclerFireBase.layoutManager = LinearLayoutManager(this)

        val adapter = ThemesItemsAdapter(items, btnDescargar, this)
        recyclerFireBase.adapter = adapter

        dialog.show()

        btnDescargar.setOnClickListener {
            btnDescargar.isEnabled = false
            txProgress.visibility = View.VISIBLE
            progressDownload.visibility = View.VISIBLE
            btnDescargar.isEnabled = false

            val storage = FirebaseStorage.getInstance()
            val storageReference = storage.reference
            val fileRef = storageReference.child("Themes/" + filePath)

            val localDirectory = File(getExternalFilesDir(null), "FingerDance/Themes")
            localDirectory.mkdirs()
            val localFile = File(localDirectory, filePath)

            val fallo = AlertDialog.Builder(this)
            fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")

            fileRef.getFile(localFile).addOnSuccessListener {
                themes.edit().putString("theme", filePath.replace(".zip", "", ignoreCase = true)).apply()
                val unzipTheme = UnzipTheme(this, filePath)
                unzipTheme.performUnzip(localFile.absolutePath)
            }.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressDownload.progress = progress
                txProgress.text = "Descargando $progress%"

                if (progress == 100) {
                    txProgress.text = "Descarga finalizada, espere por favor..."
                }
            }.addOnFailureListener {
                fallo.show()
            }
        }
    }

    override fun onItemClick(item: String) {
        filePath = item
    }

    fun getListOfThemesItems(): MutableList<String> {
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        val themesRef = storageReference.child("Themes")
        val listThemes = mutableListOf<String>()

        themesRef.listAll().addOnSuccessListener { listResult ->
                val itemsList = listResult.items.map { item ->
                    item.name
                }
                for (itemName in itemsList) {
                    listThemes.add(itemName)
                }
            }
            .addOnFailureListener { exception ->
                //println("Error al obtener la lista de archivos: ${exception.message}")
            }
        return listThemes
    }

    fun getListChannels(): MutableList<String> {
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        val channelsRef = storageReference.child("Channels")
        val listChannels = mutableListOf<String>()

        channelsRef.listAll().addOnSuccessListener { listResult ->
            val itemsList = listResult.items.map { item ->
                item.name
            }
            for (itemName in itemsList) {
                listChannels.add(itemName)
            }
        }.addOnFailureListener { exception ->
                println("Error al obtener la lista de archivos: ${exception.message}")
            }
        return listChannels
    }

    private class ThemesItemsAdapter(
        private val items: List<String>,
        private val btnDescargar: Button,
        private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<ThemesItemsAdapter.ViewHolder>() {
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

            fun bind(item: String) {
                textViewItem.text = item
                if (absoluteAdapterPosition == selectedItemPosition) {
                    textViewItem.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.teal_200))
                    btnDescargar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.progreso_textview_moderno))
                } else {
                    textViewItem.setBackgroundColor(Color.TRANSPARENT)
                }
                itemView.setOnClickListener {

                    val previousSelectedItemPosition = selectedItemPosition
                    selectedItemPosition = adapterPosition

                    if (previousSelectedItemPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previousSelectedItemPosition)
                    }

                    notifyItemChanged(selectedItemPosition)

                    val selectedItemText = textViewItem.text.toString()
                    itemClickListener.onItemClick(selectedItemText)
                    btnDescargar.isEnabled = true
                }
            }
        }
    }

    fun pxToSp(px: Float, context: Context): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    override fun onBackPressed() {
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

interface ItemClickListener {
    fun onItemClick(item: String)
}