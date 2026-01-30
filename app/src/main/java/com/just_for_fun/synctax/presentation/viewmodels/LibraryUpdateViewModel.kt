package com.just_for_fun.synctax.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.utils.LibraryUpdateChecker
import com.just_for_fun.synctax.core.utils.LibraryUpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling library update checks and UI state
 */
class LibraryUpdateViewModel : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * Check for library updates
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            try {
                val result = LibraryUpdateChecker.checkForNewPipeUpdate()

                if (result.needsUpdate) {
                    _updateState.value = UpdateState.UpdateAvailable(result)
                } else if (result.error != null) {
                    _updateState.value = UpdateState.Error(result.error)
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset the state (useful when dialog is dismissed)
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        object UpToDate : UpdateState()
        data class UpdateAvailable(val result: LibraryUpdateResult) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}