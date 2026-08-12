package com.fingerdance

import android.R.attr.fadingEdgeLength
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import android.provider.OpenableColumns
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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

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
        viewPager.offscreenPageLimit = 3

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
    private val selectedChannels = linkedMapOf<String, String>()
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
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode != 1001 ||
            resultCode != AppCompatActivity.RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return

        val selectedFileName = getFileNameFromUri(uri)

        val progressBackground =
            txProgressDownloadChannel.background as? LayerDrawable

        val progressLayer =
            progressBackground
                ?.findDrawableByLayerId(R.id.progress) as? ClipDrawable

        linearTextProgressChannel.visibility = View.VISIBLE
        txProgressDownloadChannel.isVisible = true
        txProgressDownloadChannel.setTextColor(Color.WHITE)

        progressLayer?.level = 0

        lifecycleScope.launch {
            var tempZip: File? = null

            try {
                txProgressDownloadChannel.text =
                    "Cargando $selectedFileName · 0%"

                tempZip = copyZipToCache(uri) { progress ->
                    if (progress != null) {
                        progressLayer?.level = progress * 100

                        txProgressDownloadChannel.text =
                            "Cargando $selectedFileName · $progress%"
                    } else {
                        txProgressDownloadChannel.text =
                            "Cargando $selectedFileName..."
                    }
                }

                progressLayer?.level = 10_000

                txProgressDownloadChannel.text =
                    "Descomprimiendo $selectedFileName..."

                val unzipSongs = UnzipSongs(
                    context = requireActivity(),
                    textView = txProgressDownloadChannel
                )

                unzipSongs.finishActivity.observe(
                    viewLifecycleOwner
                ) { shouldFinish ->
                    if (shouldFinish == true) {
                        requireActivity().finish()
                    }
                }

                unzipSongs.performUnzip(
                    rutaZip = tempZip.absolutePath,
                    deleteZip = true
                )

                txProgressDownloadChannel.text =
                    "Recargando canales. Este proceso puede tomar " +
                            "varios segundos, no cierre esta pantalla."

                unzipSongs.reloadChannelsAndFinish(
                    message = "Instalación completada."
                )

            } catch (e: Exception) {
                e.printStackTrace()

                tempZip?.delete()
                progressLayer?.level = 0

                txProgressDownloadChannel.text =
                    "Error al instalar $selectedFileName"

                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(
                        e.message
                            ?: "No se pudo instalar el canal."
                    )
                    .setPositiveButton("Aceptar", null)
                    .show()
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
        val checksContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            removeAllViews()
        }

        selectedChannels.clear()

        listFilesDrive.forEach { channel ->
            val driveFileId = channel.second
            val checkBox = CheckBox(requireContext())
            checkBox.text = channel.first
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(Color.WHITE)
            checkBox.textSize = 16f //pxToSp((height / 55).toFloat(), requireContext())
            checkBox.typeface = Typeface.DEFAULT_BOLD
            checkBox.setShadowLayer(6f, 0f, 0f, Color.rgb(0, 229, 255))
            checkBox.setPadding(28, 22, 28, 22)
            checkBox.background = neonCardDrawable(Color.argb(130, 8, 12, 32), Color.rgb(0, 229, 255), 2)
            checkBox.buttonTintList = ColorStateList.valueOf(Color.rgb(0, 229, 255))
            checkBox.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }

            checkBox.setOnCheckedChangeListener { item, isChecked ->
                val channelSelected = item.findViewById<CheckBox>(checkBox.id)
                val itemList = listFilesDrive.find { it.first == channelSelected.text.toString() }

                if (isChecked) {
                    selectedChannels[itemList!!.first] = driveFileId
                } else {
                    selectedChannels.remove(itemList!!.first)
                }

                val hasSelection = selectedChannels.isNotEmpty()

                downloadButtonChannel.isEnabled = hasSelection
                downloadButtonChannel.setTextColor(
                    if (hasSelection) {
                        Color.WHITE
                    } else {
                        Color.GRAY
                    }
                )

                if (hasSelection) {
                    view?.findViewById<ImageView>(
                        R.id.arrowIndicator
                    )?.visibility = View.GONE

                    view?.findViewById<TextView>(
                        R.id.txSlide
                    )?.visibility = View.GONE
                }
            }

            checksContainer.addView(checkBox)
        }

        scrollChannels.removeAllViews()
        scrollChannels.addView(checksContainer)

        downloadButtonChannel.text = "DESCARGAR SELECCIONADOS"
        downloadButtonChannel.isEnabled = false
        downloadButtonChannel.setTextColor(Color.GRAY)
    }

    private fun setupDeleteChannel(view: View) {
        val btnDeleteChannel = view.findViewById<Button>(R.id.deleteChannel)
        btnDeleteChannel.setOnClickListener {
            val selectedChannelsDelete = linkedSetOf<String>()
            val colorDanger = Color.rgb(255, 55, 95)
            val colorCard = Color.argb(170, 25, 8, 16)

            val mainContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(35, 25, 35, 20)
            }

            val description = TextView(requireContext()).apply {
                text = "Selecciona uno o varios canales que deseas eliminar."
                setTextColor(Color.rgb(220, 220, 220))
                textSize = 15f
                setPadding(10, 0, 10, 20)
            }
            mainContainer.addView(description)

            val warning = TextView(requireContext()).apply {
                text = "Los canales seleccionados serán eliminados permanentemente."
                setTextColor(Color.rgb(255, 120, 140))
                typeface = Typeface.DEFAULT_BOLD
                textSize = 14f
                setPadding(10, 0, 10, 25)
            }
            mainContainer.addView(warning)

            val scrollView = ScrollView(requireContext()).apply {
                isFillViewport = false
                isVerticalScrollBarEnabled = true
            }

            val channelsContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            listChannels.forEach { channel ->
                val channelName = channel.nombre
                val channelRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(18, 10, 22, 10)
                    background = neonCardDrawable(colorCard, colorDanger, 2)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 12)
                    }
                }

                val checkBox = CheckBox(requireContext()).apply {
                    id = View.generateViewId()
                    text = ""
                    buttonTintList = ColorStateList.valueOf(colorDanger)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val channelText = TextView(requireContext()).apply {
                    text = channelName
                    setTextColor(Color.WHITE)
                    textSize = 16f //pxToSp((height / 55).toFloat(), requireContext())
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER_VERTICAL
                    setShadowLayer(5f, 0f, 0f, colorDanger)
                    setPadding(12, 16, 10, 16)
                    maxLines = 2
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedChannelsDelete.add(channelName)
                    } else {
                        selectedChannelsDelete.remove(channelName)
                    }
                }

                channelRow.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }

                channelText.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }

                channelRow.addView(checkBox)
                channelRow.addView(channelText)
                channelsContainer.addView(channelRow)
            }

            scrollView.addView(channelsContainer)
            scrollView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (height * 0.40f).toInt()
            )
            mainContainer.addView(scrollView)

            val scrollIndicator = TextView(requireContext()).apply {
                text = "↓  Desliza para ver más canales"
                setTextColor(Color.rgb(255, 120, 140))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 14, 0, 4)
                visibility = View.INVISIBLE
            }
            mainContainer.addView(scrollIndicator)

            scrollView.post {
                scrollIndicator.visibility = if(scrollView.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
            }

            scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
                scrollIndicator.visibility = if(scrollView.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
            }

            val dialogEliminar = AlertDialog.Builder(requireContext(), R.style.DeleteChannelDialog)
                .setTitle("Eliminar canales")
                .setView(mainContainer)
                .setCancelable(false)
                .setPositiveButton("ELIMINAR", null)
                .setNegativeButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialogEliminar.setOnShowListener {
                dialogEliminar.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorDanger)
                dialogEliminar.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)

                dialogEliminar.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (selectedChannelsDelete.isEmpty()) {
                        Toast.makeText(requireContext(), "Selecciona al menos un canal para eliminar", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val total = selectedChannelsDelete.size
                    val names = selectedChannelsDelete.joinToString(separator = "\n") { "• $it" }
                    val message = if (total == 1) {
                        "Se eliminará el siguiente canal:\n\n$names\n\nEsta acción no se puede revertir."
                    } else {
                        "Se eliminarán $total canales:\n\n$names\n\nEsta acción no se puede revertir."
                    }

                    val dialogConfirmar = AlertDialog.Builder(requireContext(), R.style.DeleteChannelDialog)
                        .setTitle(if (total == 1) "Eliminar canal" else "Eliminar canales")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("ELIMINAR") { _, _ ->
                            selectedChannelsDelete.forEach { channelName ->
                                deleteChannelFolder(channelName)
                                db.deleteCanal(channelName)
                            }

                            themes.edit().putString("allTunes", "").apply()

                            val deletedCount = selectedChannelsDelete.size
                            Toast.makeText(
                                requireContext(),
                                if (deletedCount == 1) {
                                    "Canal eliminado correctamente"
                                } else {
                                    "$deletedCount canales eliminados correctamente"
                                },
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        }
                        .setNegativeButton("CANCELAR", null)
                        .create()

                    dialogConfirmar.setOnShowListener {
                        dialogConfirmar.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorDanger)
                        dialogConfirmar.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
                    }

                    dialogConfirmar.show()
                }
            }

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
            if (selectedChannels.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos un canal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totalSelected = selectedChannels.size
            val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            builder.setTitle("Aviso")
            builder.setMessage(
                if (totalSelected == 1) {
                    "Se descargará el canal seleccionado. " +
                            "Se recomienda usar una conexión Wi-Fi."
                } else {
                    "Se descargarán $totalSelected canales. " +
                            "Se recomienda usar una conexión Wi-Fi."
                }
            )

            builder.setCancelable(false)

            builder.setPositiveButton("Aceptar") { _, _ ->
                when {
                    isUsingWifi(requireContext()) -> { downloadSelectedChannels() }
                    isUsingMobileData(requireContext()) -> { mostrarDialogoDatosMoviles() }
                    else -> {
                        Toast.makeText(requireContext(), "No se detectó una conexión a Internet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            builder.setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun downloadSelectedChannels() {
        if (selectedChannels.isEmpty()) {
            return
        }

        val channelsToDownload = selectedChannels.toList()

        downloadButtonChannel.isEnabled = false
        scrollChannels.isEnabled = false

        linearTextProgressChannel.visibility = View.VISIBLE
        txProgressDownloadChannel.isVisible = true
        txProgressDownloadChannel.setTextColor(Color.WHITE)

        val progressBackground = txProgressDownloadChannel.background as? LayerDrawable

        val progressLayer = progressBackground?.findDrawableByLayerId(R.id.progress) as? ClipDrawable

        lifecycleScope.launch {
            val unzipSongs = UnzipSongs(
                context = requireActivity(),
                textView = txProgressDownloadChannel
            )

            var completedChannels = 0

            try {
                channelsToDownload.forEachIndexed { index, channel ->
                    val fileName = channel.first
                    val driveFileId = channel.second

                    val currentNumber = index + 1
                    val totalChannels = channelsToDownload.size

                    progressLayer?.level = 0

                    txProgressDownloadChannel.text = "Descargando $fileName \nCanal $currentNumber de $totalChannels · 0%"

                    val downloadedFile = downloadChannelFromDrive(
                        fileId = driveFileId,
                        fileName = fileName,
                        context = requireContext()
                    ) { progress ->

                        progressLayer?.level = progress * 100

                        txProgressDownloadChannel.text =
                            "Descargando $fileName \nCanal $currentNumber de $totalChannels · $progress%"
                    }

                    if (downloadedFile == null) {
                        throw IllegalStateException(
                            "No se pudo descargar $fileName"
                        )
                    }

                    progressLayer?.level = 10_000

                    txProgressDownloadChannel.text =
                        "Descomprimiendo $fileName\n Canal $currentNumber de $totalChannels"

                    unzipSongs.performUnzip(
                        rutaZip = downloadedFile.absolutePath,
                        deleteZip = true
                    )

                    completedChannels++
                }
                progressLayer?.level = 10_000
                txProgressDownloadChannel.text = "Recargando canales. Este proceso puede tomar varios segundos, no cierre esta pantalla."

                unzipSongs.finishActivity.observe(
                    viewLifecycleOwner
                ) { shouldFinish ->
                    if (shouldFinish == true) {
                        requireActivity().finish()
                    }
                }

                unzipSongs.reloadChannelsAndFinish(
                    message = if (completedChannels == 1) {
                        "Canal instalado correctamente."
                    } else {
                        "$completedChannels canales instalados correctamente."
                    }
                )

            } catch (e: Exception) {
                e.printStackTrace()

                progressLayer?.level = 0

                txProgressDownloadChannel.text =
                    "Ocurrió un error después de instalar $completedChannels de ${channelsToDownload.size} canales."

                downloadButtonChannel.isEnabled = true
                scrollChannels.isEnabled = true

                AlertDialog.Builder(requireContext())
                    .setTitle("Error de descarga")
                    .setMessage(e.message ?: "No fue posible completar la descarga.")
                    .setPositiveButton("Aceptar", null)
                    .show()
            }
        }
    }

    private suspend fun downloadChannelFromDrive(fileId: String, fileName: String, context: Context, progressCallback: (Int) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isChannel = true
                        showAlertFail(fileId)
                    }

                    return@withContext null
                }

                val body = response.body
                    ?: return@withContext null

                val totalSize = body.contentLength()

                val localDirectory = File(
                    context.getExternalFilesDir(null),
                    "FingerDance/Songs/Channels/"
                )

                if (!localDirectory.exists()) {
                    localDirectory.mkdirs()
                }

                val safeFileName = if (
                    fileName.endsWith(".zip", ignoreCase = true)
                ) {
                    fileName
                } else {
                    "$fileName.zip"
                }

                val localFile = File(
                    localDirectory,
                    safeFileName
                )

                BufferedInputStream(
                    body.byteStream()
                ).use { input ->

                    BufferedOutputStream(
                        FileOutputStream(localFile)
                    ).use { output ->

                        val buffer = ByteArray(65_536)

                        var bytesRead: Int
                        var totalBytes = 0L
                        var lastProgress = -1

                        while (
                            input.read(buffer)
                                .also { bytesRead = it } != -1
                        ) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            if (totalSize > 0L) {
                                val progress =
                                    ((100L * totalBytes) / totalSize)
                                        .toInt()
                                        .coerceIn(0, 100)

                                if (progress != lastProgress) {
                                    lastProgress = progress

                                    withContext(Dispatchers.Main) {
                                        progressCallback(progress)
                                    }
                                }
                            }
                        }

                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    progressCallback(100)
                }

                localFile
            }

        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                isChannel = true
                showAlertFail(fileId)
            }

            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "canal.zip"

        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (
                nameIndex >= 0 &&
                cursor.moveToFirst()
            ) {
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        var size = -1L

        requireContext().contentResolver.query(
            uri, arrayOf(OpenableColumns.SIZE), null, null, null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(
                OpenableColumns.SIZE
            )

            if (
                sizeIndex >= 0 &&
                cursor.moveToFirst() &&
                !cursor.isNull(sizeIndex)
            ) {
                size = cursor.getLong(sizeIndex)
            }
        }

        return size
    }

    private suspend fun copyZipToCache(
        uri: Uri,
        progressCallback: (Int?) -> Unit
    ): File = withContext(Dispatchers.IO) {

        val fileName = getFileNameFromUri(uri)

        val safeFileName = if (
            fileName.endsWith(".zip", ignoreCase = true)
        ) {
            fileName
        } else {
            "$fileName.zip"
        }

        val tempZip = File(
            requireContext().cacheDir,
            safeFileName
        )

        val totalSize = getFileSizeFromUri(uri)

        val inputStream =
            requireContext()
                .contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "No se pudo abrir el archivo seleccionado"
                )

        BufferedInputStream(inputStream).use { input ->
            BufferedOutputStream(
                tempZip.outputStream()
            ).use { output ->

                val buffer = ByteArray(65_536)

                var bytesRead: Int
                var copiedBytes = 0L
                var lastProgress = -1

                while (
                    input.read(buffer)
                        .also { bytesRead = it } != -1
                ) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead

                    if (totalSize > 0L) {
                        val progress =
                            ((copiedBytes * 100L) / totalSize)
                                .toInt()
                                .coerceIn(0, 100)

                        if (progress != lastProgress) {
                            lastProgress = progress

                            withContext(Dispatchers.Main) {
                                progressCallback(progress)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            progressCallback(null)
                        }
                    }
                }

                output.flush()
            }
        }

        tempZip
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
        AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setMessage("Está utilizando datos móviles. ¿Desea descargar los canales seleccionados?")
            .setPositiveButton("Aceptar") { _, _ ->
                downloadSelectedChannels()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        viewLifecycleOwner.lifecycleScope.launch {
            val listThemes = withContext(Dispatchers.IO) {
                val dir = requireContext().getExternalFilesDir("/FingerDance/Themes/")
                val result = ArrayList<ThemeItem>()
                if (dir == null) {
                    return@withContext result
                }
                val listRutasThemes = mutableListOf<String>()
                dir.walkTopDown().forEach { file ->
                    if (file.isDirectory && file.name.equals("GraphicsStatics", ignoreCase = true)
                    ) {
                        var hasFiles = false
                        var totalSize = 0L
                        file.walkTopDown().forEach { child ->
                            if (child.isFile) {
                                hasFiles = true
                                totalSize += child.length()
                            }
                        }

                        if (hasFiles && totalSize > 10_000_000L) {
                            val ruta = file.absolutePath.removeSuffix(File.separator + "GraphicsStatics")
                            listRutasThemes.add(ruta)
                        }
                    }
                }

                listRutasThemes.sort()
                listRutasThemes.forEach { ruta ->
                    val themeFolder = File(ruta)
                    val nombre = themeFolder.name
                    val rutaBanner = File(themeFolder, "logo_theme.png").absolutePath
                    result.add(ThemeItem(rutaBanner, nombre, false)
                    )
                }
                result
            }
            if (!isAdded) {
                return@launch
            }

            recyclerThemes.adapter = ThemesAdapter(listThemes, btnGuardar)
        }
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
        linearPadsD.layoutParams.width = (width * 0.8).toInt()
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

        val pathImg1 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad.png")!!
            .absolutePath

        val pathImg2 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad_m.png")!!
            .absolutePath

        val pathImg3 = requireContext()
            .getExternalFilesDir("/FingerDance/PadsD/arrows_pad_bg_n.png")!!
            .absolutePath

        val listPadsD = arrayListOf<String>(pathImg1, pathImg2, pathImg3)

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
                        selectedTypePadD = tempTypePadD,
                        listPathsImagesD = listPadsD
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

    private fun createImgPadsD(type: Int, listPathsImagesD: ArrayList<String>): Bitmap {
        return when (type) {
            0 -> createBitmapPadsD(listPathsImagesD[0])
            1 -> createBitmapPadsD(listPathsImagesD[1])
            2 -> createBitmapPadsD(listPathsImagesD[2])
            else -> createBitmapPadsD(listPathsImagesD[0])
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
        listPathsImagesD: ArrayList<String>,
        onChanged: (Int) -> Unit,
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
            setImageBitmap(createImgPadsD(0, listPathsImagesD))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val imageView2 = ImageView(requireContext()).apply {
            setImageBitmap(createImgPadsD(1, listPathsImagesD))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            ).apply {
                bottomMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val imageView3 = ImageView(requireContext()).apply {
            setImageBitmap(createImgPadsD(2, listPathsImagesD))
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt(),
                medidaFlechas.toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 18
            }

            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        imagesContainer.addView(imageView1)
        imagesContainer.addView(imageView2)
        imagesContainer.addView(imageView3)

        mainContainer.addView(radioGroup)
        mainContainer.addView(imagesContainer)

        var isInternalInitializing = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            var newTypePadD = 0
            when (checkedId) {
                rbtn1.id -> {
                    newTypePadD = 0
                    setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[0]))
                }
                rbtn2.id -> {
                    newTypePadD = 1
                    setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[1]))
                }
                rbtn3.id -> {
                    newTypePadD = 2
                    setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[2]))
                }
                else -> {
                    newTypePadD = 0
                    setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[0]))
                }
            }

            if (!isInternalInitializing) {
                onChanged(newTypePadD)
            }
        }

        when (selectedTypePadD) {
            0 -> {
                radioGroup.check(rbtn1.id)
                setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[0]))
            }
            1 -> {
                radioGroup.check(rbtn2.id)
                setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[1]))
            }
            2 -> {
                radioGroup.check(rbtn3.id)
                setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[2]))
            }
            else -> {
                radioGroup.check(rbtn1.id)
                setImageToPadD(imageView, createPadCrossBitmap(listPathsImagesD[0]))
            }
        }

        isInternalInitializing = false

        linearPadsD.addView(mainContainer)
        linearPadsD.addView(imageView)
    }

    private fun setImageToPadD(image: ImageView, bitmap: Bitmap){
        image.setImageBitmap(bitmap)
    }

    private fun createPadCrossBitmap(path: String): Bitmap {
        val original = BitmapFactory.decodeFile(path)

        val columns = 5
        val frameWidth = original.width / columns
        val frameHeight = original.height

        val frames = ArrayList<Bitmap>()

        for (i in 0 until columns) {
            frames.add(
                Bitmap.createBitmap(
                    original,
                    i * frameWidth,
                    0,
                    frameWidth,
                    frameHeight
                )
            )
        }

        val finalWidth = (width * 0.72f).toInt()
        val finalHeight = (height * 0.36f).toInt()

        val result = createBitmap(finalWidth, finalHeight)

        val canvas = Canvas(result)

        val sidePadWidth = (finalWidth * 0.34f).toInt()
        val sidePadHeight = (finalHeight * 0.50f).toInt()

        val centerPadWidth = (finalWidth * 0.34f).toInt()
        val centerPadHeight = (finalHeight * 0.45f).toInt()

        val topY = 0
        val bottomY = (finalHeight * 0.50f).toInt()
        val centerY = (finalHeight * 0.25f).toInt()

        val leftX = 0
        val rightX = finalWidth - sidePadWidth
        val centerX = (finalWidth - centerPadWidth) / 2

        val topLeft = frames[1].scale(sidePadWidth, sidePadHeight)

        val topRight = frames[3].scale(sidePadWidth, sidePadHeight)

        val bottomLeft = frames[0].scale(sidePadWidth, sidePadHeight)

        val bottomRight = frames[4].scale(sidePadWidth, sidePadHeight)

        val center = frames[2].scale(centerPadWidth, centerPadHeight)

        canvas.drawBitmap(topLeft, leftX.toFloat(), topY.toFloat(), null)
        canvas.drawBitmap(topRight, rightX.toFloat(), topY.toFloat(), null)

        canvas.drawBitmap(bottomLeft, leftX.toFloat(), bottomY.toFloat(), null)
        canvas.drawBitmap(bottomRight, rightX.toFloat(), bottomY.toFloat(), null)

        canvas.drawBitmap(center, centerX.toFloat(), centerY.toFloat(), null)

        return result
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
                setTitle("Cortar Canción")
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
            text = "Aplicacion creada por y para fans"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        val txMyVersionNoteSkins = view.findViewById<TextView>(R.id.txMyVersionNoteSkin).apply {
            id = View.generateViewId()
            text = "Versión: $versionUpdate"
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

class OptionsPagerAdapter(activity: FragmentActivity, private val fragments: List<Fragment>, ) : FragmentStateAdapter(activity) {

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]
}

