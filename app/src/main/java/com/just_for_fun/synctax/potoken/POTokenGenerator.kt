package com.just_for_fun.synctax.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Generates YouTube PO Token by loading YouTube in a hidden WebView
 * and extracting token from ytcfg JavaScript object.
 *
 * Usage:
 * POGenerator(context).generate { token ->
 *      // token received here
 * }
 */

class POGenerator(private val context: Context) {

    private var callback: ((String?) -> Unit)? = null
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun generate(onResult: (String?) -> Unit) {
        callback = onResult

        webView = WebView(context)
        webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString =
                "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Mobile Safari/537.36"
        }

        webView?.addJavascriptInterface(Bridge(), "AndroidPO")

        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectExtractorJS()
            }
        }

        // Load lightweight mobile page
        webView?.loadUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
    }

    private fun injectExtractorJS() {
        val js = """
            (function() {
                try {
                    let token = (
                        ytcfg?.data_?.WEB_PLAYER_CONTEXT_CONFIGS?.WEB_PLAYER_CONTEXT_CONFIG_ID_KEVLAR_WATCH?.po_token ||
                        ytcfg?.data_?.WEB_PLAYER_CONTEXT_CONFIGS?.WEB_PLAYER_CONTEXT_CONFIG_ID_INNERTUBE?.po_token ||
                        ytcfg?.data_?.po_token
                    );

                    if (token) {
                        AndroidPO.sendToken(token);
                    } else {
                        AndroidPO.sendToken(null);
                    }
                } catch(e) {
                    AndroidPO.sendToken(null);
                }
            })();
        """.trimIndent()

        webView?.evaluateJavascript(js, null)
    }

    inner class Bridge {
        @JavascriptInterface
        fun sendToken(token: String?) {
            Handler(Looper.getMainLooper()).post {
                callback?.invoke(token)
                destroy()
            }
        }
    }

    private fun destroy() {
        try {
            webView?.removeJavascriptInterface("AndroidPO")
            webView?.destroy()
        } catch (_: Exception) { }
        webView = null
    }
}
