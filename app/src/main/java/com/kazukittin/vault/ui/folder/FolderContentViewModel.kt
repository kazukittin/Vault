package com.kazukittin.vault.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.remote.SynoFolder
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderContentViewModel(
    private val folderRepository: FolderRepository,
    private val folderPath: String
) : ViewModel() {

    private val _items = MutableStateFlow<List<SynoFolder>>(emptyList())
    val items: StateFlow<List<SynoFolder>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadContents()
    }

    private fun loadContents() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedItems = folderRepository.getFolderContents(folderPath)
            _items.value = fetchedItems
            _isLoading.value = false
        }
    }

    fun getThumbnailUrl(path: String): String? {
        return folderRepository.getThumbnailUrl(path)
    }
}
