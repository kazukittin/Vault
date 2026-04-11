package com.kazukittin.vault.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.remote.SynoFolder
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.regex.Pattern

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

    enum class SortOrder {
        DEFAULT, RJ_ASC, RJ_DESC
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

    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // メタデータ（サークル名など）のキャッシュ
    private val _metadataCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadataCache = _metadataCache.asStateFlow()

    // ソート済みのアイテムリスト
    val sortedItems = combine(_items, _sortOrder) { items, order ->
        when (order) {
            SortOrder.DEFAULT -> items
            SortOrder.RJ_ASC -> items.sortedWith(rjComparator(true))
            SortOrder.RJ_DESC -> items.sortedWith(rjComparator(false))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    fun getDlSiteThumbnailUrl(title: String): String? = folderRepository.getDlSiteThumbnailUrl(title)

    fun changeSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    private fun rjComparator(ascending: Boolean) = Comparator<SynoFolder> { a, b ->
        val numA = extractRjNumber(a.name)
        val numB = extractRjNumber(b.name)
        
        if (numA == numB) {
            a.name.compareTo(b.name)
        } else {
            if (ascending) numA.compareTo(numB) else numB.compareTo(numA)
        }
    }

    private fun extractRjNumber(name: String): Long {
        val match = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(name)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: Long.MAX_VALUE
    }

    /** RJコードからサークル名を非同期で取得してキャッシュを更新する */
    fun fetchMetadataForRJItems() {
        val rjItems = _items.value.mapNotNull { item ->
            val match = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(item.name)
            match?.value?.uppercase()
        }.distinct()

        viewModelScope.launch {
            rjItems.forEach { rj ->
                if (!_metadataCache.value.containsKey(rj)) {
                    val metadata = folderRepository.getDlSiteMetadata(rj)
                    if (metadata != null) {
                        val circle = metadata.maker_name ?: "Unknown"
                        _metadataCache.value = _metadataCache.value + (rj to circle)
                    }
                }
            }
        }
    }
}
