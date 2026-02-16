package com.fingerdance

import android.app.AlertDialog
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient

class WebDownloadDialog(private val context: Context) {
    private lateinit var dialog: AlertDialog
    private lateinit var webView: WebView

    fun show(fileName: String = "Descargando...") {
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            loadUrl("file:///android_asset/download_dialog.html")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    evaluateJavascript("javascript:setFileName('$fileName')", null)
                }
            }
        }

        dialog = AlertDialog.Builder(context)
            .setView(webView)
            .setCancelable(false)
            .show()
    }

    fun updateProgress(progress: Int) {
        webView.evaluateJavascript("javascript:updateProgress($progress)", null)
    }

    fun dismiss() {
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean = ::dialog.isInitialized && dialog.isShowing
}
