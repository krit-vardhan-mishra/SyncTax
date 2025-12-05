package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.presentation.utils.AlbumColors
import com.just_for_fun.synctax.presentation.utils.DynamicColorExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DynamicBackgroundViewModel(application: Application) : AndroidViewModel(application) {
    private val _albumColors = MutableStateFlow(AlbumColors.default())
    val albumColors: StateFlow<AlbumColors> = _albumColors

    fun updateAlbumArt(albumArtUri: String?) {
        viewModelScope.launch {
            val colors = DynamicColorExtractor.extractColorsFromAlbumArt(
                getApplication(),
                albumArtUri
            )
            _albumColors.value = colors
        }
    }
}
