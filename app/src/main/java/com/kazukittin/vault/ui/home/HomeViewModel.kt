package com.kazukittin.vault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.FolderEntity
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val folderRepository: FolderRepository) : ViewModel() {

    // DBから常に最新の「すべてのフォルダ」を受け取る
    val allFolders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // DBから常に最新の「ピン留めフォルダ」を受け取る
    val pinnedFolders: StateFlow<List<FolderEntity>> = folderRepository.getPinnedFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // ViewModelが生成されたら（ホーム画面が開いたら）自動でNASへ同期しに行く
        syncData()
    }

    private fun syncData() {
        viewModelScope.launch {
            folderRepository.syncFolders()
        }
    }
}
