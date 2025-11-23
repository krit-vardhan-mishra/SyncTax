package com.just_for_fun.synctax.potoken

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult

/**
 * Helper class to generate YouTube poTokens for bypassing bot detection.
 * 
 * Usage example:
 * ```kotlin
 * // In a coroutine scope:
 * val result = PoTokenHelper.generatePoToken(context, "YOUR_VIDEO_ID")
 * result.onSuccess { poTokenResult ->
 *     Log.d("PoToken", "Visitor Data: ${poTokenResult.visitorData}")
 *     Log.d("PoToken", "Player Token: ${poTokenResult.playerPoToken}")
 *     Log.d("PoToken", "Streaming Token: ${poTokenResult.streamingPoToken}")
 *     
 *     // Use these tokens in your YouTube API requests:
 *     // - Add visitor_data header
 *     // - Add po_token for player requests
 *     // - Add po_token for streaming requests
 * }.onFailure { error ->
 *     Log.e("PoToken", "Failed to generate token", error)
 * }
 * ```
 */
object PoTokenHelper {
    private const val TAG = "PoTokenHelper"
    
    /**
     * Generate a poToken result for the given video ID.
     * This creates a WebView instance, runs BotGuard, and generates all necessary tokens.
     * 
     * @param context Application or Activity context
     * @param videoId YouTube video ID (e.g., "dQw4w9WgXcQ")
     * @return Result containing PoTokenResult with visitor_data and tokens, or an exception
     */
    suspend fun generatePoToken(context: Context, videoId: String): Result<PoTokenResult> = withContext(Dispatchers.Main) {
        runCatching {
            val generator = NewPipePoTokenGenerator()
            val result = generator.getWebClientPoToken(videoId)
                ?: throw Exception("Failed to generate poToken - WebView not supported or error occurred")
            
            Log.d(TAG, "Successfully generated poToken for video: $videoId")
            Log.d(TAG, "Visitor Data: ${result.visitorData}")
            result
        }
    }
    
    /**
     * Alternative: Use the standalone PoTokenWebView directly for more control.
     * This gives you a reusable generator that can create multiple tokens.
     * Remember to call generator.close() when done!
     * 
     * Example:
     * ```kotlin
     * val generator = PoTokenWebView.getNewPoTokenGenerator(context)
     * try {
     *     // Generate streaming token (must be done first)
     *     val visitorData = "YOUR_VISITOR_DATA" // Get from Innertube API
     *     val streamingToken = generator.generatePoToken(visitorData)
     *     
     *     // Generate player token for specific video
     *     val playerToken = generator.generatePoToken("VIDEO_ID")
     *     
     *     // Use tokens...
     * } finally {
     *     // Must be called on main thread!
     *     withContext(Dispatchers.Main) {
     *         generator.close()
     *     }
     * }
     * ```
     */
    suspend fun getReusableGenerator(context: Context): PoTokenWebView {
        return PoTokenWebView.getNewPoTokenGenerator(context)
    }
}
