package com.just_for_fun.synctax.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility class for showing consistent snackbars throughout the app
 */
object SnackbarUtils {

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
     */
    @Composable
    fun ShowSnackbarComposable(
        snackbarHostState: SnackbarHostState,
        message: String? = null,
        isLoading: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short,
        containerColor: Color = Color.Unspecified
    ) {
        val scope = rememberCoroutineScope()
        ShowSnackbar(scope, snackbarHostState, message, isLoading, duration, containerColor)
    }
}
