package com.nguyendevs.ecolens.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CustomDialogUtils {

    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String? = null,
        cancelText: String? = null,
        onConfirm: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirmation, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val dialog = AlertDialog.Builder(context).setView(dialogView).setCancelable(true).create()

        tvTitle.text = title
        tvMessage.text = message
        if (confirmText != null) btnConfirm.text = confirmText
        if (cancelText != null) btnCancel.text = cancelText

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener { dialog.dismiss(); onConfirm() }
        dialog.show()
    }

    private fun buildGbifMapHtml(taxonKey: Int, speciesName: String): String {
        return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body, #map { width: 100%; height: 100%; background: #0f172a; }
            #map { transform: translate3d(0,0,0); }
            #title {
                position: absolute; top: 16px; left: 50%;
                transform: translateX(-50%); z-index: 1000;
                background: rgba(15, 23, 42, 0.85); color: #f8fafc;
                padding: 10px 20px; border-radius: 16px;
                font-size: 14px; font-weight: 600; font-family: system-ui, -apple-system, sans-serif;
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
                border: 1px solid rgba(255, 255, 255, 0.1);
                backdrop-filter: blur(8px);
                white-space: nowrap; max-width: 85vw;
                overflow: hidden; text-overflow: ellipsis;
                pointer-events: none;
            }
            .leaflet-container { background: #0f172a !important; }
        </style>
    </head>
    <body>
        <div id="title"><i>$speciesName</i></div>
        <div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
            var map = L.map('map', { 
                zoomControl: false,
                attributionControl: false,
                fadeAnimation: true,
                zoomAnimation: true,
                markerZoomAnimation: true,
                preferCanvas: true
            }).setView([20, 0], 2);
            
            L.control.zoom({ position: 'bottomright' }).addTo(map);

            L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
                maxZoom: 20,
                updateWhenIdle: true,
                keepBuffer: 2
            }).addTo(map);

            L.tileLayer(
                'https://api.gbif.org/v2/map/occurrence/density/{z}/{x}/{y}@2x.png' +
                '?srs=EPSG:3857&taxonKey=$taxonKey&style=purpleYellow.point',
                {
                    opacity: 0.9,
                    maxZoom: 14,
                    updateWhenIdle: true
                }
            ).addTo(map);
        </script>
    </body>
    </html>
    """.trimIndent()
    }

    private fun configureWebView(webView: WebView) {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setAllowFileAccess(true)
        settings.setAllowContentAccess(true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        webView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                val radius = 24 * view.resources.displayMetrics.density
                outline.setRoundRect(0, (-radius).toInt(), view.width, view.height, radius)
            }
        }
        webView.clipToOutline = true

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun createDialog(
        context: Context,
        dialogView: View,
        widthRatio: Double = 0.95,
        heightRatio: Double = 0.85
    ): android.app.Dialog {
        val dialog = android.app.Dialog(context)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val metrics = context.resources.displayMetrics
        dialog.window?.setLayout(
            (metrics.widthPixels * widthRatio).toInt(),
            (metrics.heightPixels * heightRatio).toInt()
        )
        return dialog
    }

    fun showGbifMap(context: Context, scientificName: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_webview, null)
        val webView = dialogView.findViewById<WebView>(R.id.webView)
        val loadingIndicator = dialogView.findViewById<View>(R.id.loadingIndicator)
        val webProgress = dialogView.findViewById<LinearProgressIndicator>(R.id.webProgress)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        val dialog = createDialog(context, dialogView, 0.95, 0.85)

        configureWebView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingIndicator.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    loadingIndicator.visibility = View.GONE
                    Toast.makeText(context, "Error: ${error?.description}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                webProgress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                webProgress.progress = newProgress
                if (newProgress > 80) loadingIndicator.visibility = View.GONE
            }
        }

        btnClose.setOnClickListener { webView.stopLoading(); dialog.dismiss() }
        dialog.setOnDismissListener { webView.stopLoading(); webView.destroy() }
        dialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cleanName = scientificName.replace(Regex("<[^>]*>"), "").trim()
                val encodedName = java.net.URLEncoder.encode(cleanName, "UTF-8")
                val apiUrl = "https://api.gbif.org/v1/species/match?name=$encodedName&strict=false"

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.iNaturalistApi.getGbifTaxonomy(apiUrl)
                }

                val usageKey = response.usageKey
                if (usageKey != null) {
                    val html = buildGbifMapHtml(usageKey, cleanName)
                    webView.loadDataWithBaseURL(
                        null,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                } else {
                    tvTitle.text = context.getString(R.string.error)
                    loadingIndicator.visibility = View.GONE
                    Toast.makeText(context, "Cannot find distribution map for this species", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
                Toast.makeText(context, "Error loading map: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showWebViewDialog(context: Context, url: String, title: String? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_webview, null)
        val webView = dialogView.findViewById<WebView>(R.id.webView)
        val loadingIndicator = dialogView.findViewById<View>(R.id.loadingIndicator)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        if (title != null) tvTitle.text = title

        val dialog = createDialog(context, dialogView, 0.95, 0.8)

        configureWebView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingIndicator.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) loadingIndicator.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress > 80) loadingIndicator.visibility = View.GONE
            }
        }

        btnClose.setOnClickListener { webView.stopLoading(); dialog.dismiss() }
        dialog.setOnDismissListener { webView.stopLoading(); webView.destroy() }
        webView.loadUrl(url)
        dialog.show()
    }
}