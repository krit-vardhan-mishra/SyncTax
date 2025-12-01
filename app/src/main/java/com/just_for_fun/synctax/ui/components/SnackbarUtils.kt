package com.just_for_fun.synctax.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility class for showing consistent snackbars throughout the app
 */
object SnackbarUtils {

    /**
     * Shows a loading snackbar with the given message
     */
    @Composable
    fun ShowLoadingSnackbar(
        snackbarHostState: SnackbarHostState,
        message: String
    ) {
        val scope = rememberCoroutineScope()
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite
            )
        }
    }

    /**
     * Dismisses the current snackbar
     */
    @Composable
    fun DismissSnackbar(snackbarHostState: SnackbarHostState) {
        val scope = rememberCoroutineScope()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}