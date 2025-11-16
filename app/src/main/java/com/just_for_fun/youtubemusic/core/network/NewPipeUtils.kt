package com.just_for_fun.youtubemusic.core.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.AudioStream
import java.net.URLDecoder

/**
 * Thin wrapper around NewPipe's extractor to decode signature-ciphered stream URLs.
 * Uses NewPipe's internal YoutubeJavaScriptPlayerManager to obtain valid signatures.
 */
object NewPipeUtils {

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    try {
                        NewPipe.init(object : Downloader() {
                            private val client = OkHttpClient.Builder().build()

                            override fun execute(request: NPRequest): NPResponse {
                                val reqBuilder = Request.Builder()
                                    .url(request.url())
                                    .method(request.httpMethod(), request.dataToSend()?.let { okhttp3.RequestBody.create(null, it) })

                                request.headers()?.forEach { (k, values) ->
                                    values?.forEach { v -> reqBuilder.addHeader(k, v) }
                                }

                                val resp = client.newCall(reqBuilder.build()).execute()

                                return NPResponse(resp.code, resp.message, resp.headers.toMultimap(), resp.body?.string(), resp.request.url.toString())
                            }
                        })
                        initialized = true
                    } catch (t: Throwable) {
                        // initialization failure will be surfaced later when trying to use the functions
                        t.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Get stream URL for a YouTube video using NewPipe's full extractor.
     * This handles all YouTube client logic, bot detection bypassing, and cipher decryption.
     */
    fun getStreamUrl(videoId: String): Result<String> = runCatching {
        ensureInitialized()
        
        val url = "https://www.youtube.com/watch?v=$videoId"
        Log.d("NewPipeUtils", "Extracting stream for: $url")
        
        val extractor = ServiceList.YouTube.getStreamExtractor(url)
        extractor.fetchPage()
        
        // Get best audio stream
        val audioStreams = extractor.audioStreams
        if (audioStreams.isNullOrEmpty()) {
            throw IllegalStateException("No audio streams available for $videoId")
        }
        
        // Sort by bitrate and get best quality
        val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("Could not determine best audio stream")
        
        val streamUrl = bestAudio.content
        Log.d("NewPipeUtils", "Found audio stream: ${bestAudio.format?.name ?: "unknown"}, bitrate: ${bestAudio.averageBitrate}")
        
        streamUrl
    }
    
    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        ensureInitialized()
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    /**
     * Given `signatureCipher` content from the player response and the videoId, extract the
     * playable stream URL by decoding the obfuscated signature.
     */
    fun getStreamUrlFromSignatureCipher(signatureCipher: String, videoId: String): Result<String> = runCatching {
        ensureInitialized()
        val params = parseQueryString(signatureCipher)
        val obfuscated = params["s"] ?: throw IllegalArgumentException("missing s parameter in signatureCipher")
        val sp = params["sp"] ?: "signature"
        val urlParam = params["url"] ?: throw IllegalArgumentException("missing url parameter in signatureCipher")

        // Use NewPipe's manager to compute the signature. There is no stable guarantee on the
        // exact method name, so try the known variants via reflection.
        val manager = YoutubeJavaScriptPlayerManager::class.java
        val sig: String = try {
            // try common API first
            val meth = manager.getMethod("getSignature", String::class.java, String::class.java)
            meth.invoke(null, obfuscated, videoId) as String
        } catch (e: Exception) {
            try {
                val meth = manager.getMethod("getSignature", String::class.java)
                meth.invoke(null, obfuscated) as String
            } catch (e2: Exception) {
                throw IllegalStateException("Failed to compute signature: ${e.message}; ${e2.message}")
            }
        }

        val decodedUrl = URLDecoder.decode(urlParam, Charsets.UTF_8.name())
        // If the URL already has query params, append using &
        val separator = if (decodedUrl.contains("?")) "&" else "?"
        return@runCatching "$decodedUrl$separator$sp=$sig"
    }

    private fun parseQueryString(query: String): Map<String, String> {
        return query.split("&").mapNotNull { pair ->
            if (pair.isBlank()) return@mapNotNull null
            val idx = pair.indexOf('=')
            if (idx < 0) return@mapNotNull null
            val key = URLDecoder.decode(pair.substring(0, idx), Charsets.UTF_8.name())
            val value = URLDecoder.decode(pair.substring(idx + 1), Charsets.UTF_8.name())
            key to value
        }.toMap()
    }
}
