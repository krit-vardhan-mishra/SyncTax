package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnlineSongsUiState(
    val history: List<OnlineListeningHistory> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class OnlineSongsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PAGE_SIZE = 20
    }

    private val database = MusicDatabase.getDatabase(application)
    private val dao = database.onlineListeningHistoryDao()

    private val _uiState = MutableStateFlow(OnlineSongsUiState())
    val uiState: StateFlow<OnlineSongsUiState> = _uiState.asStateFlow()

    private var currentOffset = 0

    init {
        loadInitialHistory()
    }

    private fun loadInitialHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                dao.getOnlineHistoryPaged(0, PAGE_SIZE).collect { history ->
                    _uiState.value = _uiState.value.copy(
                        history = history,
                        isLoading = false,
                        hasMore = history.size == PAGE_SIZE
                    )
                    currentOffset = history.size
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load online history: ${e.message}"
                )
            }
        }
    }

    fun loadMoreHistory() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
            try {
                dao.getOnlineHistoryPaged(currentOffset, PAGE_SIZE).collect { newHistory ->
                    val updatedHistory = _uiState.value.history + newHistory
                    _uiState.value = _uiState.value.copy(
                        history = updatedHistory,
                        isLoadingMore = false,
                        hasMore = newHistory.size == PAGE_SIZE
                    )
                    currentOffset += newHistory.size
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = "Failed to load more history: ${e.message}"
                )
            }
        }
    }

    fun deleteHistory(videoId: String) {
        viewModelScope.launch {
            try {
                dao.deleteByVideoId(videoId)
                // Refresh the list
                loadInitialHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete history: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}