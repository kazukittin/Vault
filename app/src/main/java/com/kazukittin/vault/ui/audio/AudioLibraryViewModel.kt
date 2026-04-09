package com.kazukittin.vault.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.WorkEntity
import com.kazukittin.vault.data.repository.AudioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AudioLibraryViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // すべての作品
    val allWorks: StateFlow<List<WorkEntity>> = combine(
        audioRepository.getAllWorks(),
        _searchQuery
    ) { works, query ->
        if (query.isBlank()) works
        else works.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.circle.contains(query, ignoreCase = true) ||
            it.cv.contains(query, ignoreCase = true) ||
            it.tags.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scan(rootPath: String) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                audioRepository.scanAudioRoot(rootPath)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun toggleFavorite(work: WorkEntity) {
        viewModelScope.launch {
            audioRepository.audioDao.updateWork(work.copy(isFavorite = !work.isFavorite))
        }
    }
}
