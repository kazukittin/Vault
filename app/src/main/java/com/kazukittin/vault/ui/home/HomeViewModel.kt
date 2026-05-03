package com.kazukittin.vault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.FolderEntity
import com.kazukittin.vault.data.repository.AuthRepository
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val folderRepository: FolderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val allFolders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pinnedFolders: StateFlow<List<FolderEntity>> = folderRepository.getPinnedFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isConnecting = MutableStateFlow(true)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    init {
        sync()
    }

    private fun sync() {
        viewModelScope.launch {
            _isConnecting.value = true
            _connectionError.value = null

            val sessionOk = authRepository.validateOrRefreshSession()
            val syncOk = if (sessionOk) folderRepository.syncFolders() else false

            _isConnecting.value = false
            if (!syncOk) {
                _connectionError.value = "NASに接続できませんでした"
            }
        }
    }

    fun retryConnection() {
        sync()
    }

    fun setFolderCategory(folderId: String, category: String?) {
        viewModelScope.launch {
            folderRepository.updateFolderCategory(folderId, category)
        }
    }
}
