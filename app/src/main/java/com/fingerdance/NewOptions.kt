package com.fingerdance

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private var fileNameChannel = ""

private fun neonCardDrawable(fillColor: Int, strokeColor: Int, strokeWidth: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 22f
        setColor(fillColor)
        setStroke(strokeWidth, strokeColor)
    }
}

class OptionsActivity : AppCompatActivity(), ItemClickListener {
    private lateinit var titleNewOptions: TextView
    private lateinit var bgNewOptions: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_new_options)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@OptionsActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                startActivity(intent)
                finish()
            }
        })

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        FirebaseApp.initializeApp(this)

        titleNewOptions = findViewById(R.id.titleNewOptions)
        bgNewOptions = findViewById(R.id.bgNewOptions)

        val bit = BitmapFactory.decodeFile(File(getExternalFilesDir(null), "FingerDance/Themes/$tema/GraphicsStatics/bg.jpg").absolutePath)
        bgNewOptions.background = bit.toDrawable(resources)

        titleNewOptions.paintFlags = titleNewOptions.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        titleNewOptions.text = "FINGER DANCE OPTIONS"
        titleNewOptions.setShadowLayer(12f, 0f, 0f, Color.CYAN)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerOptions)

        val fragments = listOf(
            CancionesFragment(),
            TemasFragment(),
            PadsFragment(),
            AjustesFragment()
        )

        val adapter = OptionsPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabTitles = listOf("Canciones", "Temas", "Pads", "Ajustes")
            val tabText = TextView(this).apply {
                text = tabTitles[position]
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                textSize = 13f
                gravity = Gravity.CENTER
            }
            tab.customView = tabText
        }.attach()

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onItemClick(item: Pair<String, String>) {
        // Implementación de ItemClickListener
    }
}

class CancionesFragment : Fragment(R.layout.options_canciones) {
    private lateinit var scrollChannels: NestedScrollView
    private lateinit var downloadButtonChannel: Button
    private lateinit var txProgressDownloadChannel: TextView
    private lateinit var linearTextProgressChannel: LinearLayout
    private var selectedValueChannel: String? = null
    private var isChannel = false
    private var nameNewChannel = ""
    private var descriptionNewChannel = ""
    private val idNewChannel = 100
    private var isSscChannel = false

    private val pickPreviewFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            saveFileToDestination(it)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 1001 || resultCode != AppCompatActivity.RESULT_OK) {
            return
        }

        val uri = data?.data ?: return

        txProgressDownloadChannel.isVisible = true
        txProgressDownloadChannel.text = "Instalando canal, espere por favor..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempZip = File(requireContext().cacheDir, "temp_channel.zip")
                requireContext().contentResolver.openInputStream(uri)
                    ?.use { input ->
                        tempZip.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                val unzipSongs = UnzipSongs(
                    requireActivity(),
                    fileNameChannel,
                    txProgressDownloadChannel,
                    "Instalación completada",
                    false
                )

                unzipSongs.performUnzip(tempZip.absolutePath)
                tempZip.delete()
                withContext(Dispatchers.Main) {
                    unzipSongs.finishActivity.observe(requireActivity()) { shouldFinish ->
                        if (shouldFinish) {
                            requireActivity().finish()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txProgressDownloadChannel.text = "Error al instalar canal"
                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("${e.message}")
                        .setPositiveButton("Aceptar") { d, _ -> d.dismiss() }
                        .create()
                        .show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadButtonChannel = view.findViewById(R.id.download_button_channel)
        scrollChannels = view.findViewById(R.id.scrollChannels)
        txProgressDownloadChannel = view.findViewById(R.id.textViewDownloadChannel)
        linearTextProgressChannel = view.findViewById(R.id.linearTextProgressChannel)
        linearTextProgressChannel.visibility = View.INVISIBLE

        val arrowIndicator = view.findViewById<ImageView>(R.id.arrowIndicator)
        val txSlide = view.findViewById<TextView>(R.id.txSlide)

        scrollChannels.layoutParams.height = (height * 0.55).toInt()

        scrollChannels.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                arrowIndicator.visibility = View.INVISIBLE
                txSlide.visibility = View.INVISIBLE
            }else{
                arrowIndicator.visibility = View.VISIBLE
                txSlide.visibility = View.VISIBLE
            }
        }

        txProgressDownloadChannel.layoutParams.width = (width / 10) * 9
        txProgressDownloadChannel.isVisible = false
        downloadButtonChannel.setTextColor(Color.GRAY)

        setupChannelsList()
        setupDeleteChannel(view)
        setupCreateChannel(view)
        setupInstallChannels(view)
        setupDownloadChannel()
    }

    private fun setupChannelsList() {
        val radioChannelsDownload = RadioGroup(requireContext()).apply {
            removeAllViews()
        }

        listFilesDrive.forEach { channel ->
            val radioButton = RadioButton(requireContext())
            radioButton.text = channel.first
            radioButton.id = View.generateViewId()
            radioButton.setTextColor(Color.WHITE)
            radioButton.textSize = pxToSp((height / 55).toFloat(), requireContext())
            radioButton.typeface = Typeface.DEFAULT_BOLD
            radioButton.setShadowLayer(6f, 0f, 0f, Color.rgb(0, 229, 255))
            radioButton.setPadding(28, 22, 28, 22)
            radioButton.background = neonCardDrawable(Color.argb(130, 8, 12, 32), Color.rgb(0, 229, 255), 2)
            radioButton.buttonTintList = ColorStateList.valueOf(Color.rgb(0, 229, 255))
            radioButton.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
            radioChannelsDownload.addView(radioButton)
        }

        scrollChannels.addView(radioChannelsDownload)

        radioChannelsDownload.setOnCheckedChangeListener { group, checkedId ->
            val channelSelected = group.findViewById<RadioButton>(checkedId)
            val itemList = listFilesDrive.find { it.first == channelSelected.text.toString() }
            selectedValueChannel = itemList!!.second
            fileNameChannel = itemList.first
            downloadButtonChannel.isEnabled = channelSelected.isChecked
            if(downloadButtonChannel.isEnabled){
                downloadButtonChannel.setTextColor(Color.WHITE)
            }
            view?.findViewById<ImageView>(R.id.arrowIndicator)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.txSlide)?.visibility = View.GONE
        }
    }

    private fun setupDeleteChannel(view: View) {
        val btnDeleteChannel = view.findViewById<Button>(R.id.deleteChannel)
        btnDeleteChannel.setOnClickListener {
            var nameChannelDelete = ""
            val layoutOptionsDelete = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val radioChannelsDelete = RadioGroup(requireContext()).apply {
                removeAllViews()
            }

            listChannels.forEach { channel ->
                val radioButton = RadioButton(requireContext())
                radioButton.text = channel.nombre
                radioButton.id = View.generateViewId()
                radioChannelsDelete.addView(radioButton)
            }

            radioChannelsDelete.setOnCheckedChangeListener { group, checkedId ->
                val channelSelected = group.findViewById<RadioButton>(checkedId)
                nameChannelDelete = channelSelected.text.toString()
            }

            layoutOptionsDelete.addView(radioChannelsDelete)
            val dialogEliminar = AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle("Eliminar Canal")
                .setMessage("Selecciona el canal que deseas eliminar. Una vez eliminado, volverás a la pantalla principal")
                .setView(layoutOptionsDelete)
                .setPositiveButton("Eliminar") { _, _ ->
                    val dialogConfirmar = AlertDialog.Builder(requireContext())
                        .setCancelable(false)
                        .setTitle("Confirmar")
                        .setMessage(if(nameChannelDelete == ""){
                            "Selecciona un canal para eliminar"
                        }else{
                            "¿Seguro que desea eliminar el canal $nameChannelDelete? Esta acción no se puede revertir."
                        })
                        .setPositiveButton("Aceptar") { d, _ ->
                            if(nameChannelDelete == ""){
                                d.dismiss()
                            }else{
                                deleteChannelFolder(nameChannelDelete)
                                db.deleteCanal(nameChannelDelete)
                                Toast.makeText(requireContext(), "El canal: $nameChannelDelete se ha eliminado", Toast.LENGTH_SHORT).show()
                                themes.edit().putString("allTunes", "").apply()
                                startActivity(Intent(requireContext(), MainActivity()::class.java))
                                requireActivity().finish()
                            }
                        }
                        .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                        .create()
                    dialogConfirmar.show()
                }
                .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
                .create()

            dialogEliminar.show()
        }
    }

    private fun setupCreateChannel(view: View) {
        val btnCreateChannel = view.findViewById<Button>(R.id.createChannel)
        btnCreateChannel.setOnClickListener {
            showCreateChannelDialog()
        }
    }

    private fun setupInstallChannels(view: View) {
        val btnInstallChannel = view.findViewById<Button>(R.id.installChannel)
        btnInstallChannel.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Instalar canal")
                .setMessage("Seleccione el archivo ZIP del canal que desea instalar.")
                .setPositiveButton("Continuar") { _, _ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                        )
                    }

                    startActivityForResult(
                        intent,
                        1001
                    )
                }

                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showCreateChannelDialog() {

        val options = arrayOf("Canal KSF", "Canal SSC", "Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("Crear Canal")
            .setItems(options) { dialog, which ->

                when (which) {
                    0 -> createChannelKSF()
                    1 -> createChannelSSC()
                    2 -> dialog.dismiss()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun setupDownloadChannel() {
        downloadButtonChannel.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            builder.setTitle("Aviso")
            builder.setMessage("Se descargara el canal seleccionado. Se recomienda usar una conexión Wi-Fi \n")
            builder.setCancelable(false)
            builder.setPositiveButton("Aceptar") { dialog, which ->
                when {
                    isUsingWifi(requireContext()) -> {
                        downloadChannel()
                    }
                    isUsingMobileData(requireContext()) -> {
                        mostrarDialogoDatosMoviles()
                    }
                }
            }
            builder.setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    private fun downloadChannel() {
        downloadButtonChannel.isEnabled = false
        txProgressDownloadChannel.text = "Conectando..."
        getDownloadChannelDrive()
    }

    private fun getDownloadChannelDrive() {
        val localDirectory = File(requireContext().getExternalFilesDir(null), "FingerDance/Songs/Channels/")
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
            val downloadedFile = downloadChannelFromDrive(selectedValueChannel!!, requireContext()) { progress ->
                txProgressDownloadChannel.text = "Descargando $progress%"
                progressLayer.level = progress * 100
                if (progress > 98) {
                    txProgressDownloadChannel.text = "Iniciando descompresión..."
                    txProgressDownloadChannel.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.fondo_textview_vibrante)
                    )
                }
                if (progress == 100) {
                    txProgressDownloadChannel.text =
                        "Recargando canales. Este proceso puede tomar varios segundos, no cierre esta pantalla."
                }
            }

            if (downloadedFile != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val unzipSongs = UnzipSongs(
                        requireActivity(),
                        fileNameChannel,
                        txProgressDownloadChannel,
                        "Recarga de canales completada.",
                        true
                    )
                    unzipSongs.performUnzip(downloadedFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        unzipSongs.finishActivity.observe(requireActivity()) { shouldFinish ->
                            if (shouldFinish) requireActivity().finish()
                        }
                    }
                }
            }
        }
    }

    private suspend fun downloadChannelFromDrive(fileId: String, context: Context, progressCallback: (Int) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"
            val request = Request.Builder()
                .url(url)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    isChannel = true
                    showAlertFail(fileId)
                }
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val totalSize = body.contentLength()
            val localDirectory = File(context.getExternalFilesDir(null), "FingerDance/Songs/Channels/")
            localDirectory.mkdirs()
            val localFile = File(localDirectory, fileNameChannel)
            val input = BufferedInputStream(body.byteStream())
            val output = BufferedOutputStream(FileOutputStream(localFile))

            val buffer = ByteArray(65536)

            var bytesRead: Int
            var totalBytes = 0L
            var lastProgress = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {

                output.write(buffer, 0, bytesRead)

                totalBytes += bytesRead

                if (totalSize > 0) {

                    val progress = ((100 * totalBytes) / totalSize).toInt()

                    if (progress != lastProgress) {
                        lastProgress = progress

                        withContext(Dispatchers.Main) {
                            progressCallback(progress)
                        }
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            return@withContext localFile

        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                isChannel = true
                showAlertFail(fileId)
            }

            return@withContext null
        }
    }

    private fun createChannelKSF() {
        isSscChannel = false
        showInputNameChannel()
    }

    private fun createChannelSSC() {
        isSscChannel = true
        showInputNameChannel()
    }

    private fun showInputNameChannel() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val editTextChannel = EditText(requireContext()).apply {
            hint = "Nombre del canal"
        }

        val editTextDescription = EditText(requireContext()).apply {
            hint = "Descripción del canal"
        }

        layout.addView(editTextChannel)
        layout.addView(editTextDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Crear canal")
            .setMessage("Por favor, ingresa el nombre y la descripción del canal")
            .setView(layout)
            .setPositiveButton("Aceptar") { _, _ ->
                if (editTextChannel.text.toString().isNotEmpty() && editTextDescription.text.toString().isNotEmpty()) {
                    nameNewChannel = idNewChannel.toString() + " - " + editTextChannel.text.toString().uppercase()
                    descriptionNewChannel = editTextDescription.text.toString()
                    if (getChannelExist()) {
                        deleteChannelFolder(nameNewChannel)
                    }
                    if (createPathNewChannel(requireContext(), nameNewChannel)) {
                        createTextIni(requireContext())
                        showSelectIconChannel()
                    } else {
                        Toast.makeText(requireContext(), "Ocurrio un error al crear el canal, verifica los permisos de almacenamiento de la aplicación", Toast.LENGTH_SHORT).show()
                        showInputNameChannel()
                    }
                } else {
                    Toast.makeText(requireContext(), "Por favor, ingresa el nombre y la descripción del canal", Toast.LENGTH_SHORT).show()
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
        val folderPath = requireContext().getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel")
        return folderPath?.exists() ?: false
    }

    private fun showSelectIconChannel() {
        val dialog = AlertDialog.Builder(requireContext())
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
        val nameInfo = if(isSscChannel) "info" else "info_ksf"
        val folderPath = context.getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel/$nameInfo/")
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
        val destinationPath = requireContext().getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel/")
        val destinationFile = File(destinationPath, if(isSscChannel)"banner.png" else "banner_ksf.png")

        try {
            val validatedInputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            validatedInputStream?.use { input ->
                val outputStream = FileOutputStream(destinationFile)
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            getSongsToCopy()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar el archivo. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            showSelectIconChannel()
        }
    }

    private fun getSongsToCopy() {
        val dialog = AlertDialog.Builder(requireContext())
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
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val targetFolder = File(requireContext().getExternalFilesDir("/FingerDance/Songs/Channels/$nameNewChannel")!!.path)
                copyAllFoldersWithContents(requireContext(), uri, targetFolder)
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

        sourceFolder.listFiles().forEach { file ->
            if (file.isDirectory) {
                val targetSubFolder = File(targetBaseFolder, file.name!!)
                targetSubFolder.mkdirs()

                copyFolderContent(context, file, targetSubFolder)
                themes.edit().putString("allTunes", "").apply()
            }
        }
        Toast.makeText(requireContext(), "Se creó el canal correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun copyFolderContent(context: Context, sourceFolder: DocumentFile, targetFolder: File) {
        sourceFolder.listFiles().forEach { file ->
            if (file.isDirectory) {
                val newTargetSubFolder = File(targetFolder, file.name!!)
                newTargetSubFolder.mkdirs()
                copyFolderContent(context, file, newTargetSubFolder)
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

    private fun createPathNewChannel(context: Context, nameFolder: String): Boolean {
        var existFolder = true
        val folderPath = context.getExternalFilesDir("/FingerDance/Songs/Channels/$nameFolder/")
        if (folderPath != null && !folderPath.exists()) {
            existFolder = folderPath.mkdirs()
        }
        return existFolder
    }

    private fun deleteChannelFolder(nameChannel: String) {
        val folderPath = requireContext().getExternalFilesDir("/FingerDance/Songs/Channels/$nameChannel/")
        folderPath?.deleteRecursively()
    }

    private fun mostrarDialogoDatosMoviles() {
        val datosMoviles = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        datosMoviles.setMessage("Está utilizando datos móviles. ¿Desea continuar?")
        datosMoviles.setPositiveButton("Aceptar") { dialog, which ->
            downloadChannel()
        }
        datosMoviles.setNegativeButton("Cancelar", null)
        datosMoviles.show()
    }

    private fun showAlertFail(idDownload: String) {
        val fileDownload = if (isChannel) "canal" else "tema"
        val messageFail = "No se pudo realizar la descarga automatica, quieres descargar el $fileDownload manualmente? " +
                "\n Si ya descargaste el $fileDownload, presiona el boton 'Cargar $fileDownload'"

        val urlManual = "https://drive.google.com/file/d/$idDownload/view?usp=drive_link"

        val alert = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setMessage(messageFail)
            .setPositiveButton("Descarga manual") { d, _ ->
                val intent = Intent(Intent.ACTION_VIEW, urlManual.toUri()).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(null)
                }
                val chooser = Intent.createChooser(intent, "Abrir con navegador")
                startActivity(chooser)
                d.dismiss()
                if (isChannel) {
                    txProgressDownloadChannel.isVisible = false
                    linearTextProgressChannel.visibility = View.GONE
                    downloadButtonChannel.isEnabled = true
                }
            }
            .setNegativeButton("Cargar $fileDownload") { d, _ ->
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

    private fun isUsingWifi(context: Context): Boolean {
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

    private fun isUsingMobileData(context: Context): Boolean {
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

    private fun pxToSp(px: Float, context: Context): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }
}

class TemasFragment : Fragment(R.layout.options_temas), ItemClickListener {
    private lateinit var recyclerThemes: RecyclerView
    private lateinit var btnMoreThemes: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnCargarTema: Button
    private var filePath = ""
    private var fileNameTheme = ""
    private var isChannel = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerThemes = view.findViewById(R.id.listThemes)
        btnMoreThemes = view.findViewById(R.id.btnMoreThemes)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnGuardar.setTextColor(Color.GRAY)
        btnGuardar.isEnabled = false
        btnCargarTema = view.findViewById(R.id.btnCargarTema)

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerThemes.layoutManager = layoutManager

        setupThemesList()
        setupButtons()
    }

    override fun onItemClick(item: Pair<String, String>) {
        filePath = item.second
        fileNameTheme = item.first
    }

    private fun setupThemesList() {
        val dir = requireContext().getExternalFilesDir("/FingerDance/Themes/")
        val listThemes = ArrayList<ThemeItem>()
        val listRutasThemes = mutableListOf<String>()

        if (dir != null) {
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

        recyclerThemes.adapter = ThemesAdapter(listThemes, btnGuardar)
    }

    private fun setupButtons() {
        btnGuardar.setOnClickListener {
            val theme = ThemesAdapter.getSelectedItem()
            if(theme.text.isNotEmpty()){
                themes.edit().putString("theme", theme.text).apply()
                themes.edit().putString("allTunes", "").apply()
                themes.edit().putString("efects", "").apply()
                listChannels.clear()
                tema = theme.text
                AppResources.isLoaded = false
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }

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

        btnMoreThemes.setOnClickListener {
            Toast.makeText(requireContext(), "Cargando...", Toast.LENGTH_SHORT).show()
            requireActivity().runOnUiThread {
                showCustomDialog()
            }
        }
    }

    private fun showCustomDialog() {
        val dialog = Dialog(requireContext(), R.style.TransparentDialog)
        dialog.setContentView(R.layout.dialog_more_themes)
        dialog.setCancelable(false)

        val recyclerFireBase = dialog.findViewById<RecyclerView>(R.id.listThemesDropBox)
        val txProgress = dialog.findViewById<TextView>(R.id.txProgress)
        val progressDownload = dialog.findViewById<ProgressBar>(R.id.progressMoreThemes)
        val btnDescargar = dialog.findViewById<Button>(R.id.btnDescargar)
        val btnCerrarDialog = dialog.findViewById<Button>(R.id.btnCerrarDialog)
        val txTitle = dialog.findViewById<TextView>(R.id.txTitle)

        val mitextoU = SpannableString("Selecciona el tema que deseas descargar.")
        mitextoU.setSpan(UnderlineSpan(), 0, mitextoU.length, 0)
        txTitle.setText(mitextoU)

        txProgress.visibility = View.INVISIBLE
        progressDownload.visibility = View.INVISIBLE
        btnDescargar.visibility = View.VISIBLE
        btnDescargar.isEnabled = false

        recyclerFireBase.layoutManager = LinearLayoutManager(requireContext())

        val adapter = ThemesItemsAdapter(listThemesDrive, btnDescargar, this)
        recyclerFireBase.adapter = adapter
        dialog.show()

        btnDescargar.setOnClickListener {
            btnDescargar.visibility = View.INVISIBLE
            txProgress.visibility = View.VISIBLE
            progressDownload.visibility = View.VISIBLE
            txProgress.text = "Conetando..."
            btnCerrarDialog.isVisible = false

            val bgaPathSC = requireContext().getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectChannel.mp4")!!.absolutePath
            if (File(bgaPathSC).isDirectory) {
                File(bgaPathSC).delete()
            }
            val bgaPathSS = requireContext().getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectSong.mp4")!!.absolutePath
            if (File(bgaPathSS).isDirectory) {
                File(bgaPathSS).delete()
            }
            downloadThemeDrive(txProgress, progressDownload)
        }

        btnCerrarDialog.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.negative_red))
        btnCerrarDialog.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun downloadThemeDrive(txProgress: TextView, progressDownload: ProgressBar) {
        val localDirectory = File(requireContext().getExternalFilesDir(null), "FingerDance/Themes/")
        localDirectory.mkdirs()
        val localFile = File(localDirectory, fileNameTheme)
        lifecycleScope.launch {
            val downloadedFile = downloadThemeFromDrive(filePath, requireContext()) { progress ->
                txProgress.text = "Descargando $progress%"
                progressDownload.progress = progress

                if (progress > 98) {
                    txProgress.text = "Iniciando descompresión..."
                    txProgress.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.fondo_textview_vibrante)
                    )
                }

                if (progress == 100) {
                    txProgress.text = "Descarga completada"
                }
            }

            if (downloadedFile != null) {
                themes.edit().putString("theme", fileNameTheme.replace(".zip", "", ignoreCase = true)).apply()
                themes.edit().putString("efects", "").apply()
                tema = fileNameTheme.replace(".zip", "", ignoreCase = true)
                withContext(Dispatchers.IO) {
                    val unzipTheme = UnzipTheme(requireActivity(), fileNameTheme)
                    unzipTheme.performUnzip(downloadedFile.absolutePath)
                }

            } else {

                Toast.makeText(requireContext(), "Error en la descarga", Toast.LENGTH_LONG).show()

            }
        }
    }

    private suspend fun downloadThemeFromDrive(fileId: String, context: Context, progressCallback: (Int) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()

            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null

            val totalSize = body.contentLength()

            val localDirectory = File(context.getExternalFilesDir(null), "FingerDance/Themes/")
            localDirectory.mkdirs()

            val localFile = File(localDirectory, fileNameTheme)

            val input = BufferedInputStream(body.byteStream())
            val output = BufferedOutputStream(FileOutputStream(localFile))

            val buffer = ByteArray(65536)

            var bytesRead: Int
            var totalBytes = 0L
            var lastProgress = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {

                output.write(buffer, 0, bytesRead)

                totalBytes += bytesRead

                if (totalSize > 0) {

                    val progress = ((100 * totalBytes) / totalSize).toInt()

                    if (progress != lastProgress) {
                        lastProgress = progress

                        withContext(Dispatchers.Main) {
                            progressCallback(progress)
                        }
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            localFile

        } catch (e: Exception) {

            null

        }
    }

    private class ThemesItemsAdapter(private val items: ArrayList<Pair<String, String>>, private val btnDescargar: Button, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<ThemesItemsAdapter.ViewHolder>() {
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

                    itemClickListener.onItemClick(item)
                    btnDescargar.isEnabled = true
                }
            }
        }
    }
}

class PadsFragment : Fragment(R.layout.options_pads) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val originalShowPadB = showPadB
        val originalHideImagesPadA = hideImagesPadA
        val originalAlphaPadB = alphaPadB
        val originalSkinPad = skinPad
        val originalTypePadD = typePadD

        var tempShowPadB = originalShowPadB
        var tempHideImagesPadA = originalHideImagesPadA
        var tempAlphaPadB = originalAlphaPadB
        var tempSkinPad = originalSkinPad
        var tempTypePadD = originalTypePadD

        var isInitializing = true

        val btnGuardarPads = view.findViewById<Button>(R.id.btnGuardarPads).apply {
            text = "Guardar y salir"
            setTextColor(Color.GRAY)
            isEnabled = false
        }

        val imgPadBSelected = view.findViewById<ImageView>(R.id.imgPadBSelected)
        imgPadBSelected.layoutParams.width = (width * 0.425).toInt()
        imgPadBSelected.visibility = View.GONE

        val padBPreviewPanel = view.findViewById<ConstraintLayout>(R.id.padBPreviewPanel)
        padBPreviewPanel.visibility = View.GONE

        fun hasChanges(): Boolean {
            return tempShowPadB != originalShowPadB ||
                    tempHideImagesPadA != originalHideImagesPadA ||
                    tempAlphaPadB != originalAlphaPadB ||
                    tempSkinPad != originalSkinPad ||
                    tempTypePadD != originalTypePadD
        }

        fun updateSaveButton() {
            if (isInitializing) return

            val changed = hasChanges()

            btnGuardarPads.isEnabled = changed
            btnGuardarPads.setTextColor(
                if (changed) Color.WHITE else Color.GRAY
            )
        }

        val switchImagePadA = view.findViewById<SwitchCompat>(R.id.showImagePadA)
        switchImagePadA.visibility = View.GONE
        switchImagePadA.layoutParams.width = (width * .75).toInt()
        switchImagePadA.isChecked = tempHideImagesPadA

        val bgPadsPictures = view.findViewById<ImageView>(R.id.bg_pads_pictures)
        val bgPads = view.findViewById<ImageView>(R.id.bg_pads)

        bgPadsPictures.visibility = View.GONE
        bgPads.visibility = View.GONE

        fun updatePadAImages() {
            bgPads.isVisible = tempShowPadB == 0
            bgPadsPictures.isVisible = tempShowPadB == 0 && !tempHideImagesPadA
        }

        val linearPadsD = view.findViewById<LinearLayout>(R.id.linearPadsD)
        linearPadsD.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        linearPadsD.gravity = Gravity.CENTER_HORIZONTAL
        linearPadsD.setPadding(0, 14, 0, 0)

        val thumbColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(Color.GREEN, Color.RED)
        )

        val trackColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(R.color.track_color_A, R.color.track_color_B)
        )

        switchImagePadA.thumbTintList = thumbColor
        switchImagePadA.trackTintList = trackColor
        switchImagePadA.setTextColor(
            if (tempHideImagesPadA) Color.GREEN else Color.RED
        )

        val txPercentAlpha = view.findViewById<TextView>(R.id.txPercentAlpha)
        txPercentAlpha.visibility = View.GONE

        val initAlpha = tempAlphaPadB * 100
        txPercentAlpha.text = "Opacidad del pad B: ${initAlpha.toInt()}%"

        val seekBarAlphaPadB = view.findViewById<SeekBar>(R.id.seekBarAlphaPadB)
        seekBarAlphaPadB.layoutParams.width = width / 2
        seekBarAlphaPadB.visibility = View.GONE
        seekBarAlphaPadB.progress = initAlpha.toInt()

        seekBarAlphaPadB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = progress / 100f

                txPercentAlpha.text = "Opacidad del pad B: $progress%"
                tempAlphaPadB = String.format("%.2f", alpha).toFloat()
                imgPadBSelected.alpha = alpha
                if (fromUser) {
                    updateSaveButton()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val recyclerPadsB = view.findViewById<RecyclerView>(R.id.recyclerPadsB)
        recyclerPadsB.layoutManager = LinearLayoutManager(requireContext())
        recyclerPadsB.visibility = View.GONE
        recyclerPadsB.layoutParams.width = width / 2
        recyclerPadsB.layoutParams.height = height / 2

        val listPadsB = getlistPadsB()

        val selectedPadB = listPadsB.find {
            it.text.equals(tempSkinPad, ignoreCase = true)
        }

        selectedPadB?.let {
            imgPadBSelected.setImageBitmap(BitmapFactory.decodeFile(it.imageRuta))
            imgPadBSelected.alpha = tempAlphaPadB
        }

        recyclerPadsB.adapter = ListPadsAdapter(listPadsB, tempSkinPad) { selectedItem ->
            imgPadBSelected.setImageBitmap(BitmapFactory.decodeFile(selectedItem.imageRuta))
            imgPadBSelected.alpha = tempAlphaPadB
            tempSkinPad = selectedItem.text
            updateSaveButton()
        }

        val padCPanel = view.findViewById<ConstraintLayout>(R.id.padCPanel)
        padCPanel.visibility = View.GONE

        val recyclerPadsC = view.findViewById<RecyclerView>(R.id.recyclerPadsC)
        recyclerPadsC.layoutManager = LinearLayoutManager(requireContext())
        recyclerPadsC.visibility = View.GONE
        recyclerPadsC.layoutParams.width = (width * 0.35).toInt()
        recyclerPadsC.layoutParams.height = height / 2

        val imgPadCBg = view.findViewById<ImageView>(R.id.imgPadCBg)
        imgPadCBg.visibility = View.GONE
        imgPadCBg.layoutParams.width = (width * 0.45).toInt()

        val imgPadC = view.findViewById<ImageView>(R.id.imgPadC)
        imgPadC.visibility = View.GONE
        imgPadC.layoutParams.width = ((width * 0.45) / 2).toInt()

        var padCAnimationJob: Job? = null
        var currentPadCAnimation: AnimationDrawable? = null

        val progressPadC = view.findViewById<ProgressBar>(R.id.progressPadC)
        progressPadC.visibility = View.GONE

        fun clearPadCAnimation() {
            currentPadCAnimation?.stop()
            currentPadCAnimation = null

            imgPadC.setImageDrawable(null)
        }

        fun animatePadCFromSpriteSheet(path: String) {
            padCAnimationJob?.cancel()

            padCAnimationJob = viewLifecycleOwner.lifecycleScope.launch {
                progressPadC.visibility = View.VISIBLE
                imgPadC.visibility = View.GONE

                clearPadCAnimation()

                val animationDrawable = withContext(Dispatchers.Default) {
                    val originalBitmap = BitmapFactory.decodeFile(path) ?: return@withContext null

                    val rows = 6
                    val frameWidth = originalBitmap.width
                    val frameHeight = originalBitmap.height / rows

                    val bpm = 120
                    val beatDurationMs = 60000 / bpm
                    val frameDurationMs = beatDurationMs / rows

                    val anim = AnimationDrawable().apply {
                        isOneShot = false
                    }

                    for (i in 0 until rows) {
                        val frame = Bitmap.createBitmap(
                            originalBitmap,
                            0,
                            i * frameHeight,
                            frameWidth,
                            frameHeight
                        )

                        val drawable = BitmapDrawable(resources, frame)
                        anim.addFrame(drawable, frameDurationMs)
                    }

                    anim
                }

                if (!isAdded) return@launch

                progressPadC.visibility = View.GONE

                if (animationDrawable != null) {
                    currentPadCAnimation = animationDrawable

                    imgPadC.visibility = View.VISIBLE
                    imgPadC.setImageDrawable(animationDrawable)

                    imgPadC.post {
                        animationDrawable.start()
                    }
                } else {
                    imgPadC.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar la animación del Pad C.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val listPadsC = getlistPadsC()

        val selectedPadC = listPadsC.find {
            it.text.equals(tempSkinPad, ignoreCase = true)
        }

        selectedPadC?.let {
            imgPadCBg.setImageBitmap(BitmapFactory.decodeFile(it.imageRuta))
            animatePadCFromSpriteSheet(it.imageRuta.replace("BG.png", "Center.png", ignoreCase = true))
        }

        recyclerPadsC.adapter = ListPadsAdapter(listPadsC, tempSkinPad) { selectedItem ->
            imgPadCBg.setImageBitmap(BitmapFactory.decodeFile(selectedItem.imageRuta))
            animatePadCFromSpriteSheet(selectedItem.imageRuta.replace("BG.png", "Center.png", ignoreCase = true))
            tempSkinPad = selectedItem.text
            updateSaveButton()
        }

        val radioGroupPads = view.findViewById<RadioGroup>(R.id.radioGroupPads)

        radioGroupPads.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPadA -> {
                    switchImagePadA.visibility = View.VISIBLE

                    tempShowPadB = 0
                    updatePadAImages()

                    recyclerPadsC.visibility = View.GONE
                    padCPanel.visibility = View.GONE
                    imgPadCBg.visibility = View.GONE
                    imgPadC.visibility = View.GONE
                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    imgPadBSelected.visibility = View.GONE
                    padBPreviewPanel.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    linearPadsD.visibility = View.GONE

                    updateSaveButton()
                }

                R.id.rbPadB -> {
                    switchImagePadA.visibility = View.GONE
                    bgPadsPictures.visibility = View.GONE
                    bgPads.visibility = View.GONE

                    recyclerPadsC.visibility = View.GONE
                    padCPanel.visibility = View.GONE
                    imgPadCBg.visibility = View.GONE
                    imgPadC.visibility = View.GONE
                    txPercentAlpha.visibility = View.VISIBLE
                    seekBarAlphaPadB.visibility = View.VISIBLE
                    imgPadBSelected.visibility = View.VISIBLE
                    padBPreviewPanel.visibility = View.VISIBLE
                    recyclerPadsB.visibility = View.VISIBLE
                    linearPadsD.visibility = View.GONE

                    tempShowPadB = 1

                    updateSaveButton()
                }

                R.id.rbPadC -> {
                    switchImagePadA.visibility = View.GONE
                    bgPadsPictures.visibility = View.GONE
                    bgPads.visibility = View.GONE

                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    imgPadBSelected.visibility = View.GONE
                    padBPreviewPanel.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    recyclerPadsC.visibility = View.VISIBLE
                    padCPanel.visibility = View.VISIBLE
                    imgPadCBg.visibility = View.VISIBLE
                    imgPadC.visibility = View.VISIBLE
                    linearPadsD.visibility = View.GONE

                    tempShowPadB = 2

                    updateSaveButton()
                }

                R.id.rbPadD -> {
                    switchImagePadA.visibility = View.GONE
                    bgPadsPictures.visibility = View.GONE
                    bgPads.visibility = View.GONE

                    txPercentAlpha.visibility = View.GONE
                    seekBarAlphaPadB.visibility = View.GONE
                    imgPadBSelected.visibility = View.GONE
                    padBPreviewPanel.visibility = View.GONE
                    recyclerPadsB.visibility = View.GONE
                    recyclerPadsC.visibility = View.GONE
                    padCPanel.visibility = View.GONE
                    imgPadCBg.visibility = View.GONE
                    imgPadC.visibility = View.GONE
                    linearPadsD.visibility = View.VISIBLE

                    tempShowPadB = 3

                    showPadsD(
                        linearPadsD = linearPadsD,
                        selectedTypePadD = tempTypePadD
                    ) { newTypePadD ->
                        tempTypePadD = newTypePadD
                        updateSaveButton()
                    }

                    updateSaveButton()
                }
            }
        }

        when (tempShowPadB) {
            0 -> radioGroupPads.check(R.id.rbPadA)
            1 -> radioGroupPads.check(R.id.rbPadB)
            2 -> radioGroupPads.check(R.id.rbPadC)
            3 -> radioGroupPads.check(R.id.rbPadD)
            else -> radioGroupPads.check(R.id.rbPadA)
        }

        switchImagePadA.setOnCheckedChangeListener { _, isChecked ->
            tempHideImagesPadA = isChecked

            switchImagePadA.setTextColor(
                if (isChecked) Color.GREEN else Color.RED
            )

            updatePadAImages()
            updateSaveButton()
        }

        isInitializing = false
        updateSaveButton()

        btnGuardarPads.setOnClickListener {
            showPadB = tempShowPadB
            hideImagesPadA = tempHideImagesPadA
            alphaPadB = tempAlphaPadB
            skinPad = tempSkinPad
            typePadD = tempTypePadD

            themes.edit()
                .putInt("showPadB", showPadB)
                .putBoolean("hideImagesPadA", hideImagesPadA)
                .putFloat("alphaPadB", alphaPadB)
                .putString("skinPad", skinPad)
                .putInt("typePadD", typePadD)
                .apply()

            Toast.makeText(
                requireContext(),
                "Configuración guardada.",
                Toast.LENGTH_SHORT
            ).show()

            btnGuardarPads.visibility = View.INVISIBLE

            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun createImgPadsD(type: Int): Bitmap {
        val pathImg1 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad.png")!!
            .absolutePath

        val pathImg2 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad_m.png")!!
            .absolutePath

        val pathImg3 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad_bg_n.png")!!
            .absolutePath

        return when (type) {
            0 -> createBitmapPadsD(pathImg1)
            1 -> createBitmapPadsD(pathImg2)
            2 -> createBitmapPadsD(pathImg3)
            else -> createBitmapPadsD(pathImg1)
        }
    }

    private fun createBitmapPadsD(path: String): Bitmap {
        val original = BitmapFactory.decodeFile(path)

        val columns = 5
        val cellWidth = original.width / columns
        val cellHeight = original.height

        val index = 2
        val x = index * cellWidth
        val y = 0

        return Bitmap.createBitmap(
            original,
            x,
            y,
            cellWidth,
            cellHeight
        )
    }
    private fun showPadsD(
        linearPadsD: LinearLayout,
        selectedTypePadD: Int,
        onChanged: (Int) -> Unit
    ) {
        linearPadsD.removeAllViews()
        linearPadsD.gravity = Gravity.CENTER_HORIZONTAL
        linearPadsD.setPadding(0, 14, 0, 0)

        val mainContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }

            setBackgroundColor(0x1AFFFFFF.toInt())
            setPadding(18, 14, 18, 14)
        }

        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 28
            }
        }

        val rbtn1 = RadioButton(requireContext()).apply {
            id = View.generateViewId()
            text = "Classic"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            textSize = 13f
        }

        val rbtn2 = RadioButton(requireContext()).apply {
            id = View.generateViewId()
            text = "PIU M"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            textSize = 13f
        }

        val rbtn3 = RadioButton(requireContext()).apply {
            id = View.generateViewId()
            text = "OmegaUp"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                medidaFlechas.toInt()
            )
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            textSize = 13f
        }

        radioGroup.addView(rbtn1)
        radioGroup.addView(rbtn2)
        radioGroup.addView(rbtn3)

        val imagesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val imageView1 = ImageView(requireContext()).apply {
            setImageBitmap(createImgPadsD(0))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val imageView2 = ImageView(requireContext()).apply {
            setImageBitmap(createImgPadsD(1))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val imageView3 = ImageView(requireContext()).apply {
            setImageBitmap(createImgPadsD(2))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        imagesContainer.addView(imageView1)
        imagesContainer.addView(imageView2)
        imagesContainer.addView(imageView3)

        mainContainer.addView(radioGroup)
        mainContainer.addView(imagesContainer)

        var isInternalInitializing = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTypePadD = when (checkedId) {
                rbtn1.id -> 0
                rbtn2.id -> 1
                rbtn3.id -> 2
                else -> 0
            }

            if (!isInternalInitializing) {
                onChanged(newTypePadD)
            }
        }

        when (selectedTypePadD) {
            0 -> radioGroup.check(rbtn1.id)
            1 -> radioGroup.check(rbtn2.id)
            2 -> radioGroup.check(rbtn3.id)
            else -> radioGroup.check(rbtn1.id)
        }

        isInternalInitializing = false

        linearPadsD.addView(mainContainer)
    }

    private fun getlistPadsB(): ArrayList<ThemeItem> {
        val dir = requireContext().getExternalFilesDir("/FingerDance/PadsB/")
        val listThemes = ArrayList<ThemeItem>()

        dir?.walkTopDown()?.forEach {
            if (it.toString().endsWith(".png", true)) {
                if (it.isFile) {
                    listThemes.add(
                        ThemeItem(
                            it.absolutePath,
                            it.name.replace(".png", "", ignoreCase = true),
                            false
                        )
                    )
                }
            }
        }

        return ArrayList(listThemes.sortedBy { it.text })
    }

    private fun getlistPadsC(): ArrayList<ThemeItem> {
        val dir = requireContext().getExternalFilesDir("/FingerDance/PadsC/")
        val listThemes = ArrayList<ThemeItem>()

        dir?.walkTopDown()?.forEach {
            if (it.toString().endsWith("BG.png", true)) {
                if (it.isFile) {
                    listThemes.add(
                        ThemeItem(
                            it.absolutePath,
                            it.parentFile.name,
                            false
                        )
                    )
                }
            }
        }

        return ArrayList(listThemes.sortedBy { it.text })
    }
}

class AjustesFragment : Fragment(R.layout.options_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val thumbColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(Color.GREEN, Color.RED)
        )

        val trackColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(R.color.track_color_A, R.color.track_color_B)
        )

        setupOfflineSwitch(view, thumbColor, trackColor)
        setupMidLineSwitch(view, thumbColor, trackColor)
        setupCounterSwitch(view, thumbColor, trackColor)
        setupBreakSongSwitch(view, thumbColor, trackColor)
        setupHorizontalMode(view, thumbColor, trackColor)
        setupPlayOrientationSingle(view)
        setupPleyOrientationHalf(view)
        val holdProgress = view.findViewById<HoldProgressView>(R.id.holdProgress)
        holdProgress.layoutParams.width = (width * 0.5).toInt()
        holdProgress.onHoldComplete = {
            themes.edit().putString("allTunes", "").apply()
        }
        setupUpdateNoteSkins(view)
    }

    private fun setupPleyOrientationHalf(view: View) {
        val optionOrientationHalf = view.findViewById<OptionStepperView>(R.id.optionOrientationHalf)
        val listOptions = listOf("PREGUNTAR", "VERTICAL", "HORIZONTAL")
        optionOrientationHalf.setOptions(
            newOptions = listOptions,
            defaultIndex = playModeHalf
        )

        optionOrientationHalf.setOnOptionChangedListener { index, value ->
            when (index) {
                0 -> {
                    // PREGUNTAR
                    playModeHalf = index
                    themes.edit().putInt("playModeHalf", playModeHalf).apply()
                }

                1 -> {
                    // VERTICAL
                    playModeHalf = index
                    themes.edit().putInt("playModeHalf", playModeHalf).apply()
                }

                2 -> {
                    // HORIZONTAL
                    playModeHalf = index
                    themes.edit().putInt("playModeHalf", playModeHalf).apply()
                }
            }
            Log.d("AjustesFragment", "playModeHalf: ${themes.getInt("playModeHalf", 0)}")
        }
    }

    private fun setupPlayOrientationSingle(view: View) {
        val optionOrientationSingle = view.findViewById<OptionStepperView>(R.id.optionOrientationSingle)
        val listOptions = listOf("PREGUNTAR", "VERTICAL", "HORIZONTAL")
        optionOrientationSingle.setOptions(
            newOptions = listOptions,
            defaultIndex = playModeSingle
        )

        optionOrientationSingle.setOnOptionChangedListener { index, value ->
            when (index) {
                0 -> {
                    // PREGUNTAR
                    playModeSingle = index
                    themes.edit().putInt("playModeSingle", playModeSingle).apply()
                }

                1 -> {
                    // VERTICAL
                    playModeSingle = index
                    themes.edit().putInt("playModeSingle", playModeSingle).apply()
                }

                2 -> {
                    // HORIZONTAL
                    playModeSingle = index
                    themes.edit().putInt("playModeSingle", playModeSingle).apply()
                }
            }
            Log.d("AjustesFragment", "playModeSingle: ${themes.getInt("playModeSingle", 0)}")
        }
    }

    private fun setupHorizontalMode(view: View, thumbColor: ColorStateList, trackColor: ColorStateList){
        val btnHorizontalMode = view.findViewById<SwitchCompat>(R.id.btnHorizontalMode)
        btnHorizontalMode.layoutParams.width = (width / 10) * 8
        btnHorizontalMode.isChecked = isHorizontalMode
        btnHorizontalMode.thumbTintList = thumbColor
        btnHorizontalMode.trackTintList = trackColor

        btnHorizontalMode.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog).apply {
                setTitle("Modo Horizontal")
                setCancelable(false)
            }
            if (!isHorizontalMode) {
                dialog.setMessage(R.string.MessageHorizontalModeOn)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnHorizontalMode.isChecked = false
                    isHorizontalMode = btnHorizontalMode.isChecked
                    themes.edit().putBoolean("isHorizontalMode", isHorizontalMode).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Activar") { d, _ ->
                    btnHorizontalMode.isChecked = true
                    isHorizontalMode = btnHorizontalMode.isChecked
                    themes.edit().putBoolean("isHorizontalMode", isHorizontalMode).apply()
                    d.dismiss()
                }
            } else {
                dialog.setMessage(R.string.MessageHorizontalModeOff)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnHorizontalMode.isChecked = true
                    isHorizontalMode = btnHorizontalMode.isChecked
                    themes.edit().putBoolean("isHorizontalMode", isHorizontalMode).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Desactivar") { d, _ ->
                    btnHorizontalMode.isChecked = false
                    isHorizontalMode = btnHorizontalMode.isChecked
                    themes.edit().putBoolean("isHorizontalMode", isHorizontalMode).apply()
                    d.dismiss()
                }
            }
            dialog.show()
        }
    }

    private fun setupBreakSongSwitch(view: View, thumbColor: ColorStateList, trackColor: ColorStateList) {
        val btnBreakSong = view.findViewById<SwitchCompat>(R.id.btnBreakSong)
        btnBreakSong.layoutParams.width = (width / 10) * 8
        btnBreakSong.isChecked = breakSong
        btnBreakSong.thumbTintList = thumbColor
        btnBreakSong.trackTintList = trackColor

        btnBreakSong.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog).apply {
                setTitle("Contador Select Song")
                setCancelable(false)
            }
            if (!breakSong) {
                dialog.setMessage(R.string.MessageBreakSongOn)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnBreakSong.isChecked = false
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnBreakSong.isChecked = true
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
            } else {
                dialog.setMessage(R.string.MessageBreakSongOff)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnBreakSong.isChecked = true
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnBreakSong.isChecked = false
                    breakSong = btnBreakSong.isChecked
                    themes.edit().putBoolean("breakSong", breakSong).apply()
                    d.dismiss()
                }
            }
            dialog.show()
        }
    }

    private fun setupCounterSwitch(view: View, thumbColor: ColorStateList, trackColor: ColorStateList) {
        val btnNoCounter = view.findViewById<SwitchCompat>(R.id.btnNoCounter)
        btnNoCounter.layoutParams.width = (width / 10) * 8
        btnNoCounter.isChecked = isCounter
        btnNoCounter.thumbTintList = thumbColor
        btnNoCounter.trackTintList = trackColor

        btnNoCounter.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog).apply {
                setTitle("Contador Select Song")
                setCancelable(false)
            }
            if (!isCounter) {
                dialog.setMessage(R.string.MessageCounterOn)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnNoCounter.isChecked = false
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnNoCounter.isChecked = true
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
            } else {
                dialog.setMessage(R.string.MessageCounterOff)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnNoCounter.isChecked = true
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnNoCounter.isChecked = false
                    isCounter = btnNoCounter.isChecked
                    themes.edit().putBoolean("isCounter", isCounter).apply()
                    d.dismiss()
                }
            }
            dialog.show()
        }
    }

    private fun setupMidLineSwitch(view: View, thumbColor: ColorStateList, trackColor: ColorStateList) {
        val btnNoteMidLine = view.findViewById<SwitchCompat>(R.id.btnNoteMidLine)
        btnNoteMidLine.layoutParams.width = (width / 10) * 8
        btnNoteMidLine.isChecked = isMidLine
        btnNoteMidLine.thumbTintList = thumbColor
        btnNoteMidLine.trackTintList = trackColor

        btnNoteMidLine.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog).apply {
                setTitle("Notas a media pantalla")
                setCancelable(false)
            }
            if (!isMidLine) {
                dialog.setMessage(R.string.MessageMidLineOn)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnNoteMidLine.isChecked = false
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnNoteMidLine.isChecked = true
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
            } else {
                dialog.setMessage(R.string.MessageMidLineOff)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnNoteMidLine.isChecked = true
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnNoteMidLine.isChecked = false
                    isMidLine = btnNoteMidLine.isChecked
                    themes.edit().putBoolean("isMidLine", isMidLine).apply()
                    d.dismiss()
                }
            }

            dialog.show()
        }
    }

    private fun setupOfflineSwitch(view: View, thumbColor: ColorStateList, trackColor: ColorStateList) {
        val btnOffline = view.findViewById<SwitchCompat>(R.id.btnOffline)
        btnOffline.layoutParams.width = (width / 10) * 8
        btnOffline.isChecked = isOffline

        btnOffline.thumbTintList = thumbColor
        btnOffline.trackTintList = trackColor
        btnOffline.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog).apply {
                setTitle("Modo Offline")
                setCancelable(false)
            }
            if (!isOffline) {
                dialog.setMessage(R.string.MessageOfflineOn)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnOffline.isChecked = false
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnOffline.isChecked = true
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
            } else {
                dialog.setMessage(R.string.MessageOfflineOff)
                dialog.setNegativeButton("Cancelar") { d, _ ->
                    btnOffline.isChecked = true
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
                dialog.setPositiveButton("Aceptar") { d, _ ->
                    btnOffline.isChecked = false
                    isOffline = btnOffline.isChecked
                    d.dismiss()
                }
            }
            dialog.show()
        }
    }

    private fun setupUpdateNoteSkins(view: View) {
        val txVersionNoteSkins = view.findViewById<TextView>(R.id.txVersionNoteSkin).apply {
            id = View.generateViewId()
            text = "Ultima versión de NoteSkins: $numberUpdateLocal"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        val txMyVersionNoteSkins = view.findViewById<TextView>(R.id.txMyVersionNoteSkin).apply {
            id = View.generateViewId()
            text = "Tu versión de NoteSkins: $versionUpdate"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        val lbDescargando = TextView(view.context).apply {
            id = View.generateViewId()
            text = "Descargando:"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.WHITE)
            visibility = View.INVISIBLE
            textSize = 16f
        }

        val progressBar = ProgressBar(view.context, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 15.dpToPx())
            visibility = View.INVISIBLE
        }

        val btnUpdateNoteskins = view.findViewById<Button>(R.id.btnUptadeNoteSkin).apply {
            visibility = if (numberUpdateLocal != numberUpdateFirebase) View.VISIBLE else View.INVISIBLE
        }

        btnUpdateNoteskins.setOnClickListener {
            btnUpdateNoteskins.isEnabled = false
            lbDescargando.visibility = View.VISIBLE
            lbDescargando.text = "Conectando..."

            CoroutineScope(Dispatchers.Main).launch {
                val downloadedFile = iniciarDescargaDrive { progress ->
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = progress
                        lbDescargando.text = "Descargando $progress%"

                        if (progress == 100) {
                            lbDescargando.text = "Descarga finalizada, espere por favor..."
                            themes.edit().putString("numberUpdateLocal", numberUpdateFirebase).apply()
                            themes.edit().putString("efects", "").apply()
                            numberUpdateLocal = numberUpdateFirebase
                        }
                    }
                }
                if (downloadedFile != null) {
                    lifecycleScope.launch {
                        val unzip = Unzip(requireActivity())
                        val rutaZip = requireContext().getExternalFilesDir("FingerDance.zip").toString()
                        unzip.performUnzip(rutaZip, "FingerDance.zip", true)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error en la descarga", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun iniciarDescargaDrive(progressCallback: (Int) -> Unit): File? {
        val fallo = AlertDialog.Builder(requireContext())
        fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/1D4sMohVuJ7aGOcSzNCijsdFGHUsAf-2R?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localFile = File(requireContext().getExternalFilesDir(null), "FingerDance.zip")

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
                    fallo.show()
                    return@withContext null
                }
            } catch (e: Exception) {
                fallo.show()
                return@withContext null
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}

class OptionsPagerAdapter(
    activity: FragmentActivity,
    private val fragments: List<Fragment>,
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]
}

