package com.just_for_fun.synctax.core.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.localization.Localization
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

    /**
     * Simple OkHttp-based downloader for NewPipe with enhanced headers to bypass bot detection
     */
    private class SimpleDownloader(clientBuilder: OkHttpClient.Builder) : Downloader() {
        private val client = clientBuilder
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        override fun execute(request: NPRequest): NPResponse {
            val dataToSend = request.dataToSend()
            val requestBuilder = Request.Builder()
                .url(request.url())
                .method(
                    request.httpMethod(),
                    if (dataToSend != null) {
                        okhttp3.RequestBody.create(null, dataToSend)
                    } else null
                )

            // Add headers from request
            request.headers().forEach { (key, values) ->
                values.forEach { value ->
                    requestBuilder.addHeader(key, value)
                }
            }
            
            // Add essential headers if not already present to bypass bot detection
            if (!request.headers().containsKey("User-Agent")) {
                requestBuilder.addHeader("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            }
            if (!request.headers().containsKey("Accept-Language")) {
                requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.9")
            }
            if (!request.headers().containsKey("Accept")) {
                requestBuilder.addHeader("Accept", "*/*")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            
            val responseHeaders = mutableMapOf<String, List<String>>()
            response.headers.names().forEach { name ->
                responseHeaders[name] = response.headers.values(name)
            }

            return NPResponse(
                response.code,
                response.message,
                responseHeaders,
                responseBody,
                request.url()
            )
        }
    }

    init {
        // Initialize NewPipe with simple OkHttp downloader
        NewPipe.init(
            SimpleDownloader(OkHttpClient.Builder()),
            Localization("en", "US")
        )
        Log.d("NewPipeUtils", "NewPipe initialized")
        initialized = true
    }

    /**
     * Get stream URL for a YouTube video using NewPipe's full extractor.
     * This handles all YouTube client logic, bot detection bypassing, and cipher decryption.
     */
    fun getStreamUrl(videoId: String): Result<String> = runCatching {
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
        Log.d(
            "NewPipeUtils",
            "Found audio stream: ${bestAudio.format?.name ?: "unknown"}, bitrate: ${bestAudio.averageBitrate}"
        )

        streamUrl
    }

    /**
     * Ensure NewPipe is initialized. Call this early in app lifecycle.
     */
    fun ensureInitialized() {
        // This will trigger object initialization if not already done
    }

    /**
     * Given `signatureCipher` content from the player response and the videoId, extract the
     * playable stream URL by decoding the obfuscated signature.
     */
    fun getStreamUrlFromSignatureCipher(signatureCipher: String, videoId: String): Result<String> =
        runCatching {
            val params = parseQueryString(signatureCipher)
            val obfuscated = params["s"]
                ?: throw IllegalArgumentException("missing s parameter in signatureCipher")
            val sp = params["sp"] ?: "signature"
            val urlParam = params["url"]
                ?: throw IllegalArgumentException("missing url parameter in signatureCipher")

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
