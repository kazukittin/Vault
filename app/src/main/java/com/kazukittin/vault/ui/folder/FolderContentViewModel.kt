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
    val folderPath: String
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 200
    }

    private val _items = MutableStateFlow<List<SynoFolder>>(emptyList())
    val items: StateFlow<List<SynoFolder>> = _items.asStateFlow()

    // 最初のページを読み込んでいる最中
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 次ページを読み込んでいる最中（スクロール末尾のインジケーター用）
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // これ以上読み込むページがないか
    private val _hasReachedEnd = MutableStateFlow(false)
    val hasReachedEnd: StateFlow<Boolean> = _hasReachedEnd.asStateFlow()

    private var currentOffset = 0
    private var totalItems = Int.MAX_VALUE  // 最初は不明なので大きな数を入れておく

    init {
        loadNextPage()
    }

    /**
     * 次のページを読み込む。
     * LazyVerticalGridの末尾に到達したときにUIから呼ばれる。
     */
    fun loadNextPage() {
        // すでに読み込み中、または全件取得済みなら何もしない
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

            // 全件取得したか、空のページが返ってきたら終了
            if (newItems.isEmpty() || currentOffset >= totalItems) {
                _hasReachedEnd.value = true
            }

            _isLoading.value = false
            _isLoadingMore.value = false
        }
    }

    fun getThumbnailUrl(path: String): String? = folderRepository.getThumbnailUrl(path)
    fun getOriginalImageUrl(path: String): String? = folderRepository.getOriginalImageUrl(path)
}
