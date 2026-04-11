package com.kazukittin.vault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.FolderEntity
import com.kazukittin.vault.data.repository.AuthRepository
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val folderRepository: FolderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // DBから常に最新の「すべてのフォルダ」を受け取る
    val allFolders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // DBから常に最新の「ピン留めフォルダ」を受け取る
    val pinnedFolders: StateFlow<List<FolderEntity>> = folderRepository.getPinnedFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // 起動時: まずセッションを検証・必要なら自動再ログイン → その後データ同期
        viewModelScope.launch {
            authRepository.validateOrRefreshSession()
            folderRepository.syncFolders()
        }
    }

    fun setFolderCategory(folderId: String, category: String?) {
        viewModelScope.launch {
            folderRepository.updateFolderCategory(folderId, category)
        }
    }
}
