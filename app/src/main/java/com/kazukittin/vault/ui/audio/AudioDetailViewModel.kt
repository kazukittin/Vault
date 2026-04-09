package com.kazukittin.vault.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.TrackEntity
import com.kazukittin.vault.data.local.db.WorkEntity
import com.kazukittin.vault.data.repository.AudioRepository
import kotlinx.coroutines.flow.*

class AudioDetailViewModel(
    private val audioRepository: AudioRepository,
    val workPath: String
) : ViewModel() {

    val work: StateFlow<WorkEntity?> = audioRepository.audioDao.getWork(workPath)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<TrackEntity>> = audioRepository.getTracksForWork(workPath)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getDownloadUrl(path: String): String? {
        return audioRepository.getDownloadUrl(path)
    }
}
