package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.data.local.entities.Format
import com.just_for_fun.synctax.data.model.FormatRecyclerView
import com.just_for_fun.synctax.core.utils.AudioProcessor
import com.just_for_fun.synctax.core.utils.FormatUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormatViewModel(application: Application) : AndroidViewModel(application) {

    private val audioProcessor = AudioProcessor(application)
    private val formatUtil = FormatUtil(application)

    private val _uiState = MutableStateFlow(FormatUiState())
    val uiState: StateFlow<FormatUiState> = _uiState.asStateFlow()

    fun loadFormats(url: String) {
        viewModelScope.launch(AppDispatchers.Network) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val formats = audioProcessor.getFormats(url)
                val recyclerViewItems = prepareFormatItems(formats, _uiState.value.currentCategory)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    formats = formats,
                    recyclerViewItems = recyclerViewItems
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load formats"
                )
            }
        }
    }

    private fun prepareFormatItems(
        formats: List<Format>,
        category: FormatUtil.FormatCategory = FormatUtil.FormatCategory.SUGGESTED
    ): List<FormatRecyclerView> {
        val items = mutableListOf<FormatRecyclerView>()

        // If no formats available, use generic fallback
        val availableFormats = if (formats.isEmpty()) {
            formatUtil.getGenericAudioFormats()
        } else {
            formats
        }

        // Filter audio formats (formats with audio codec)
        val audioFormats = availableFormats.filter { format ->
            formatUtil.isAudioOnly(format)
        }

        // Filter video formats (formats with video codec)
        val videoFormats = availableFormats.filter { format ->
            !formatUtil.isAudioOnly(format)
        }

        // Apply category filtering
        val filteredAudioFormats = formatUtil.filterFormatsByCategory(
            audioFormats,
            category,
            isAudioDownload = true
        )

        val filteredVideoFormats = if (videoFormats.isNotEmpty()) {
            formatUtil.filterFormatsByCategory(
                videoFormats,
                category,
                isAudioDownload = false
            )
        } else {
            emptyList()
        }

        // Add video formats section (if available)
        if (filteredVideoFormats.isNotEmpty()) {
            items.add(FormatRecyclerView(label = "VIDEO"))
            items.addAll(filteredVideoFormats.map { FormatRecyclerView(format = it) })
        }

        // Add audio formats section
        if (filteredAudioFormats.isNotEmpty()) {
            items.add(FormatRecyclerView(label = "AUDIO"))
            items.addAll(filteredAudioFormats.map { FormatRecyclerView(format = it) })
        }

        return items
    }

    fun refreshFormats(url: String) {
        loadFormats(url)
    }

    fun selectFormat(format: Format) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedFormat = null)
    }

    fun setCategory(category: FormatUtil.FormatCategory) {
        val recyclerViewItems = prepareFormatItems(_uiState.value.formats, category)
        _uiState.value = _uiState.value.copy(
            currentCategory = category,
            recyclerViewItems = recyclerViewItems
        )
    }

    fun cycleCategory() {
        val nextCategory = when (_uiState.value.currentCategory) {
            FormatUtil.FormatCategory.SUGGESTED -> FormatUtil.FormatCategory.ALL
            FormatUtil.FormatCategory.ALL -> FormatUtil.FormatCategory.SMALLEST
            FormatUtil.FormatCategory.SMALLEST -> FormatUtil.FormatCategory.GENERIC
            FormatUtil.FormatCategory.GENERIC -> FormatUtil.FormatCategory.SUGGESTED
        }
        setCategory(nextCategory)
    }

    fun getCategoryName(): String {
        return when (_uiState.value.currentCategory) {
            FormatUtil.FormatCategory.ALL -> "ALL"
            FormatUtil.FormatCategory.SUGGESTED -> "SUGGESTED"
            FormatUtil.FormatCategory.SMALLEST -> "SMALLEST"
            FormatUtil.FormatCategory.GENERIC -> "GENERIC"
        }
    }
}

data class FormatUiState(
    val isLoading: Boolean = false,
    val formats: List<Format> = emptyList(),
    val recyclerViewItems: List<FormatRecyclerView> = emptyList(),
    val selectedFormat: Format? = null,
    val error: String? = null,
    val currentCategory: FormatUtil.FormatCategory = FormatUtil.FormatCategory.SUGGESTED
)
