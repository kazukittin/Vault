package com.kazukittin.vault.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.remote.SynoFolder
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * フォルダ画面と写真ビューアーで共有されるViewModel。
 * MainActivity レベルで生成され、フォルダ遷移のたびに reset() を呼ぶ。
 */
class FolderContentViewModel(
    private val folderRepository: FolderRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 200
    }

    var folderPath: String = ""
        private set

    private val _items = MutableStateFlow<List<SynoFolder>>(emptyList())
    val items: StateFlow<List<SynoFolder>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasReachedEnd = MutableStateFlow(false)
    val hasReachedEnd: StateFlow<Boolean> = _hasReachedEnd.asStateFlow()

    private var currentOffset = 0
    private var totalItems = Int.MAX_VALUE
    private var isFirstLoadDone = false

    /**
     * 新しいフォルダに遷移するたびに呼ぶ。
     * 同じパスなら何もしない（写真ビューアーから戻ってきた場合など）。
     */
    fun navigateTo(path: String) {
        if (path == folderPath && isFirstLoadDone) return
        folderPath = path
        // 状態をリセット
        _items.value = emptyList()
        currentOffset = 0
        totalItems = Int.MAX_VALUE
        _hasReachedEnd.value = false
        isFirstLoadDone = false
        loadNextPage()
    }

    /**
     * スクロール末尾に到達したときにUIから呼ぶ。
     */
    fun loadNextPage() {
        if (_isLoadingMore.value || _hasReachedEnd.value) return

        viewModelScope.launch {
            if (currentOffset == 0) {
                _isLoading.value = true
            } else {
                _isLoadingMore.value = true
            }

            val (newItems, total) = folderRepository.getFolderContents(
                folderPath = folderPath,
                offset = currentOffset,
                limit = PAGE_SIZE
            )

            totalItems = total
            currentOffset += newItems.size
            _items.value = _items.value + newItems

            if (newItems.isEmpty() || currentOffset >= totalItems) {
                _hasReachedEnd.value = true
            }

            isFirstLoadDone = true
            _isLoading.value = false
            _isLoadingMore.value = false
        }
    }

    fun getThumbnailUrl(path: String): String? = folderRepository.getThumbnailUrl(path)
    fun getOriginalImageUrl(path: String): String? = folderRepository.getOriginalImageUrl(path)
}
