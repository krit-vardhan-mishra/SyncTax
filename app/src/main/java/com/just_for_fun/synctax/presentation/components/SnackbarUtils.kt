package com.just_for_fun.synctax.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Import the global snackbar host state
import com.just_for_fun.synctax.globalSnackbarHostState

// CompositionLocal for global snackbar host state
val LocalSnackbarHostState =
    compositionLocalOf<androidx.compose.material3.SnackbarHostState?> { null }

/**
 * Utility class for showing consistent snackbars throughout the app
 */
object SnackbarUtils {
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Unified snackbar function that handles showing, loading, and dismissing snackbars
     */
    fun ShowSnackbar(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        message: String? = null,
        isLoading: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        containerColor: Color = Color.Unspecified // Default theme color
    ) {
        scope.launch {
            if (message.isNullOrEmpty()) {
                // Dismiss current snackbar
                snackbarHostState.currentSnackbarData?.dismiss()
            } else {
                // Show snackbar
                val actualDuration = if (isLoading) SnackbarDuration.Indefinite else duration
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = actualDuration
                )
            }
        }
    }

    /**
     * Composable wrapper for ShowSnackbar that provides the coroutine scope automatically
     * and uses global snackbar host state if available
     */
    @Composable
    fun ShowSnackbarComposable(
        snackbarHostState: SnackbarHostState? = null,
        message: String? = null,
        isLoading: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        containerColor: Color = Color.Unspecified
    ) {
        val scope = rememberCoroutineScope()
        val globalHostState = LocalSnackbarHostState.current
        val hostState =
            snackbarHostState ?: globalHostState ?: return // If no host state, do nothing
        ShowSnackbar(scope, hostState, message, isLoading, duration, containerColor)
    }

    /**
     * Non-composable function to show snackbar using global host state
     * Can be called from any context (including LaunchedEffect lambdas)
     */
    fun showGlobalSnackbar(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        val hostState = globalSnackbarHostState ?: return
        scope.launch {
            hostState.showSnackbar(message = message, duration = duration)
        }
    }
}