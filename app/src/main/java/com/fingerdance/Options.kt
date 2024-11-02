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
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ActivityOptionsBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class Options : AppCompatActivity(), ItemClickListener {

    private lateinit var bgOptions : LinearLayout
    private lateinit var titleOptions : TextView
    //private lateinit var titleTemas : TextView
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
    private lateinit var btnNoteSkins : Button

    private lateinit var btnCalibrity : Button

    private lateinit var layoutTemas : ConstraintLayout
    private lateinit var linearTextProgressChannel : LinearLayout
    private lateinit var layoutCanciones : ConstraintLayout
    private lateinit var layoutNoteSkins : ConstraintLayout

    private lateinit var items: MutableList<String>

    private lateinit var recyclerViewListChannels: RecyclerView
    private lateinit var downloadButtonChannel: Button
    private var selectedValueChannel: String? = null

    private lateinit var txProgressDownloadChannel : TextView
    //private val handler = Handler(Looper.getMainLooper())
    private var progressMaxWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_options)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        FirebaseApp.initializeApp(this)

        //tema = themes.getString("theme", "default")

        titleOptions = findViewById(R.id.titleOptions)
        //titleTemas = findViewById(R.id.btnThemes)
        //listThemes = findViewById(R.id.listThemes)
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
        btnNoteSkins = findViewById(R.id.btnNoteSkins)
        btnCalibrity = findViewById(R.id.button2)

        btnTemas.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        btnCanciones.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        btnNoteSkins.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)

        layoutTemas = findViewById(R.id.layoutThemes)
        layoutCanciones = findViewById(R.id.layoutCanciones)
        layoutNoteSkins = findViewById(R.id.layoutNoteSkins)
        linearTextProgressChannel = findViewById(R.id.linearTextProgressChannel)

        var textSize = pxToSp((width/10).toFloat(), this)
        titleOptions.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        titleOptions.paintFlags = titleOptions.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        textSize = pxToSp((width/18).toFloat(), this)
        //titleTemas.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        layoutTemas.visibility = View.GONE
        layoutCanciones.visibility = View.GONE
        layoutNoteSkins.visibility = View.GONE

        val open = resources.getDrawable(android.R.drawable.arrow_down_float, null)
        val close = resources.getDrawable(android.R.drawable.arrow_up_float, null)

        items = getListOfThemesItems()

        downloadButtonChannel = findViewById(R.id.download_button_channel)
        recyclerViewListChannels = findViewById(R.id.recycler_view_list_channels)
        layoutCanciones.layoutParams.height = (height / 2)
        recyclerViewListChannels.layoutParams.height = (height / 2.5).toInt()

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

        //downloadButtonChannel.isEnabled = selectedValueChannel != null

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

        btnTemas.setOnClickListener{
            if(layoutTemas.visibility == View.VISIBLE){
                layoutTemas.visibility = View.GONE
                btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }else{
                layoutTemas.visibility = View.VISIBLE
                layoutCanciones.visibility = View.GONE
                layoutNoteSkins.visibility = View.GONE
                btnTemas.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
            }
        }

        btnCanciones.setOnClickListener {
            if(!isUsingWifi(this) && !isUsingMobileData(this)){
                mostrarDialogoSinConexion()
                //btnCanciones.performClick()
            }else{
                if(layoutCanciones.visibility == View.VISIBLE){
                    layoutCanciones.visibility = View.GONE
                    btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
                }else{
                    layoutTemas.visibility = View.GONE
                    layoutCanciones.visibility = View.VISIBLE
                    layoutNoteSkins.visibility = View.GONE
                    btnCanciones.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
                }
            }

        }

        btnNoteSkins.setOnClickListener {
            if(layoutNoteSkins.visibility == View.VISIBLE){
                layoutNoteSkins.visibility = View.GONE
                btnNoteSkins.setCompoundDrawablesWithIntrinsicBounds(null , null, open, null)
            }else{
                layoutTemas.visibility = View.GONE
                layoutCanciones.visibility = View.GONE
                layoutNoteSkins.visibility = View.VISIBLE
                btnNoteSkins.setCompoundDrawablesWithIntrinsicBounds(null , null, close, null)
            }
        }

        btnCalibrity.setOnClickListener {
            val intent = Intent(this, CalibrationActivity::class.java)
            startActivity(intent)
        }

        val dir = getExternalFilesDir("/FingerDance/Themes/")
        val listThemes = ArrayList<ThemeItem>()
        val listRutasThemes = mutableListOf<String>()
        if (dir != null){
            dir.walkTopDown().forEach {
                if(it.toString().endsWith("logo_theme.png", true)){
                    when {
                        it.isFile -> {
                            var ruta: String = it.toString().replace("/logo_theme.png", "", ignoreCase = true)
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
                unzipTheme.performUnzip(localFile.absolutePath)
            }
        }.addOnFailureListener {
            fallo.show()
        }
    }

    private fun mostrarDialogoSinConexion() {
        val noWifi = AlertDialog.Builder(this)
        noWifi.setMessage("No hay conexión a Internet. Reintentar?")
        //noWifi.setView(R.layout.dialog_layout)
        noWifi.setPositiveButton("Reintentar") { dialog, which ->
            //downloadButtonChannel.performClick()
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
/*
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
    }
*/
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