package com.tvfut.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tvfut.app.databinding.ActivityPlayerBinding
import java.io.ByteArrayInputStream

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var adBlocker: AdBlocker
    private val streams = LinkedHashSet<String>()
    private var pageHost: String? = null

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        adBlocker = AdBlocker(this)
        pageHost = Uri.parse(url).host
        binding.toolbar.title = title.ifBlank { getString(R.string.app_name) }
        binding.toolbar.setNavigationOnClickListener { handleBack() }
        binding.toolbar.inflateMenu(R.menu.player_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    binding.webView.reload()
                    true
                }

                R.id.action_external -> {
                    openExternal(binding.webView.url ?: url)
                    true
                }

                R.id.action_copy -> {
                    copyLink(binding.webView.url ?: url)
                    true
                }

                else -> false
            }
        }

        binding.btnPlayStream.setOnClickListener { showStreamPicker() }
        binding.btnRetry.setOnClickListener {
            binding.errorBox.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE
            binding.webView.loadUrl(url)
        }
        binding.errorBox.visibility = View.GONE

        setupWebView(url)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(startUrl: String) {
        val webView = binding.webView
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.loadsImagesAutomatically = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        val userAgent = settings.userAgentString
        if (!userAgent.isNullOrBlank()) {
            settings.userAgentString = userAgent.replace("; wv", "")
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val target = request.url
                val scheme = target.scheme?.lowercase()
                if (scheme != "http" && scheme != "https") return true
                val blocked = adBlocker.isBlocked(target.host, target.path)
                if (!blocked && request.isForMainFrame) {
                    pageHost = target.host
                }
                return blocked
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val target = request.url
                if (adBlocker.isBlocked(target.host, target.path)) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                val currentPage = pageHost
                if (adBlocker.strictMode && currentPage != null && !request.isForMainFrame) {
                    val host = target.host
                    if (host != null && !SiteUtils.sameSite(host, currentPage)) {
                        val path = target.path?.lowercase().orEmpty()
                        val media = StreamUtils.isMediaUrl(target.toString()) ||
                            path.endsWith(".ts") ||
                            path.endsWith(".m4s") ||
                            path.endsWith(".aac")
                        if (!media) {
                            return WebResourceResponse(
                                "text/plain",
                                "utf-8",
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                    }
                }

                if (StreamUtils.isMediaUrl(target.toString())) {
                    synchronized(streams) { streams.add(target.toString()) }
                    runOnUiThread { updateStreamBar() }
                }
                return null
            }

            override fun onPageStarted(
                view: WebView,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                if (!url.isNullOrBlank()) {
                    pageHost = Uri.parse(url).host
                }
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                injectCosmetic(view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectCosmetic(view)
                binding.progress.visibility = View.GONE
                binding.errorBox.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    binding.progress.visibility = View.GONE
                    binding.webView.visibility = View.INVISIBLE
                    binding.errorBox.visibility = View.VISIBLE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progress.progress = newProgress
                binding.progress.visibility =
                    if (newProgress in 0..99) View.VISIBLE else View.GONE
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank() && title.length < 80) {
                    binding.toolbar.title = title
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean = false

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                binding.fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                binding.fullscreenContainer.visibility = View.VISIBLE
                binding.toolbar.visibility = View.GONE
                binding.streamBar.visibility = View.GONE
                binding.progress.visibility = View.GONE
                setSystemBarsVisible(false)
            }

            override fun onHideCustomView() {
                hideFullscreen()
            }
        }

        webView.loadUrl(startUrl)
    }

    private fun injectCosmetic(webView: WebView) {
        if (!adBlocker.enabled) return
        val script = adBlocker.cosmeticScript()
        if (script.isNotBlank()) {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun updateStreamBar() {
        val hasStreams = synchronized(streams) { streams.isNotEmpty() }
        binding.streamBar.visibility =
            if (hasStreams && customView == null) View.VISIBLE else View.GONE
    }

    private fun showStreamPicker() {
        val list = synchronized(streams) { streams.toList() }
        if (list.isEmpty()) {
            Toast.makeText(this, R.string.error_text, Toast.LENGTH_SHORT).show()
            return
        }
        if (list.size == 1) {
            playDirect(list[0])
            return
        }
        val labels = list.mapIndexed { index, stream ->
            "${index + 1}. ${stream.take(70)}"
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.streams_title)
            .setItems(labels) { _, which -> playDirect(list[which]) }
            .show()
    }

    private fun playDirect(url: String) {
        binding.webView.evaluateJavascript(
            "document.querySelectorAll('video').forEach(function(v){try{v.pause();}catch(e){}});",
            null
        )
        startActivity(
            Intent(this, DirectPlayerActivity::class.java)
                .putExtra(DirectPlayerActivity.EXTRA_URL, url)
                .putExtra(DirectPlayerActivity.EXTRA_TITLE, binding.toolbar.title?.toString())
        )
    }

    private fun handleBack() {
        val callback = customViewCallback
        if (customView != null) {
            hideFullscreen()
            callback?.onCustomViewHidden()
            return
        }
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }

    private fun hideFullscreen() {
        customView?.let { binding.fullscreenContainer.removeView(it) }
        binding.fullscreenContainer.removeAllViews()
        binding.fullscreenContainer.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE
        binding.webView.visibility = View.VISIBLE
        customView = null
        customViewCallback = null
        setSystemBarsVisible(true)
        updateStreamBar()
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun openExternal(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyLink(url: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("enlace", url))
        Toast.makeText(this, R.string.copy_title, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }
}
