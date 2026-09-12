package com.fingerdance

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient

class WebDownloadDialog(private val context: Context) {

    private lateinit var dialog: Dialog
    private lateinit var webView: WebView

    private var pageLoaded = false
    private var pendingProgress = 0
    private var pendingFileName = ""

    fun show(fileName: String = "Descargando...") {

        pendingFileName = fileName
        pendingProgress = 0
        pageLoaded = false

        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        webView = WebView(context).apply {

            setBackgroundColor(Color.TRANSPARENT)

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    pageLoaded = true

                    val safeName = pendingFileName
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")

                    evaluateJavascript(
                        "setFileName('$safeName');",
                        null
                    )

                    evaluateJavascript(
                        "updateProgress($pendingProgress);",
                        null
                    )
                }
            }

            loadUrl("file:///android_asset/download_dialog.html")
        }

        dialog.setContentView(webView)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        dialog.window?.apply {

            setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

            setGravity(Gravity.CENTER)

            val metrics = context.resources.displayMetrics

            val width = (metrics.widthPixels * 0.92f).toInt()
            val height = (metrics.heightPixels * 0.75f).toInt()

            setLayout(
                width,
                height
            )
        }
    }

    fun updateProgress(progress: Int) {

        pendingProgress = progress.coerceIn(0, 100)

        if (!::webView.isInitialized || !pageLoaded) return

        webView.post {

            if (!::webView.isInitialized) return@post

            webView.evaluateJavascript(
                "updateProgress($pendingProgress);",
                null
            )
        }
    }

    fun dismiss() {

        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }

        pageLoaded = false
    }

    fun isShowing(): Boolean {
        return ::dialog.isInitialized && dialog.isShowing
    }
}