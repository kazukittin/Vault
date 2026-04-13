package com.kazukittin.vault.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.remote.SynoFolder
import com.kazukittin.vault.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.kazukittin.vault.data.remote.DlSiteProductInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    var folderCategory: String? = null
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

    // メタデータ（サークル名、声優など）のキャッシュ
    private val _metadataCache = MutableStateFlow<Map<String, DlSiteProductInfo>>(emptyMap())
    val metadataCache = _metadataCache.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCircle = MutableStateFlow<String?>(null)
    val selectedCircle = _selectedCircle.asStateFlow()

    private val _selectedAuthor = MutableStateFlow<String?>(null)
    val selectedAuthor = _selectedAuthor.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    // 利用可能なフィルタ候補を計算
    val availableFilters = _metadataCache.map { cache ->
        val circles = cache.values.mapNotNull { it.maker_name }.distinct().sorted()
        val authors = cache.values.flatMap { it.creaters?.author ?: emptyList() }.mapNotNull { it.name }.distinct().sorted()
        val tags = cache.values.flatMap { it.genres ?: emptyList() }.mapNotNull { it.name }.distinct().sorted()
        Triple(circles, authors, tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyList(), emptyList()))

    // 検索・フィルタ・ソートを適用したアイテムリスト
    val displayItems = combine(
        listOf(_items, _sortOrder, _searchQuery, _metadataCache, _selectedCircle, _selectedAuthor, _selectedTag)
    ) { args ->
        val items = args[0] as List<SynoFolder>
        val order = args[1] as SortOrder
        val query = args[2] as String
        val cache = args[3] as Map<String, DlSiteProductInfo>
        val selCircle = args[4] as String?
        val selAuthor = args[5] as String?
        val selTag = args[6] as String?

        var filtered = items.filter { item ->
            val rjMatch = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(item.name)
            val rjCode = rjMatch?.value?.uppercase()
            val info = if (rjCode != null) cache[rjCode] else null
            
            // 1. テキスト検索
            val matchesQuery = if (query.isEmpty()) true else {
                item.name.contains(query, ignoreCase = true) ||
                info?.let {
                    it.work_name?.contains(query, ignoreCase = true) == true ||
                    it.maker_name?.contains(query, ignoreCase = true) == true ||
                    it.creaters?.voice_by?.any { v -> v.name?.contains(query, ignoreCase = true) == true } == true
                } ?: false
            }

            // 2. 属性フィルタ
            val matchesCircle = selCircle == null || info?.maker_name == selCircle
            val matchesAuthor = selAuthor == null || info?.creaters?.author?.any { it.name == selAuthor } == true
            val matchesTag = selTag == null || info?.genres?.any { it.name == selTag } == true
            
            matchesQuery && matchesCircle && matchesAuthor && matchesTag
        }

        when (order) {
            SortOrder.DEFAULT -> filtered
            SortOrder.RJ_ASC -> filtered.sortedWith(rjComparator(true))
            SortOrder.RJ_DESC -> filtered.sortedWith(rjComparator(false))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentOffset = 0
    private var totalItems = Int.MAX_VALUE
    private var isFirstLoadDone = false

    /**
     * 新しいフォルダに遷移するたびに呼ぶ。
     * 同じパスなら何もしない（写真ビューアーから戻ってきた場合など）。
     */
    fun navigateTo(path: String, category: String? = null) {
        if (path == folderPath && isFirstLoadDone) return
        folderPath = path
        folderCategory = category
        // 状態をリセット
        _items.value = emptyList()
        _metadataCache.value = emptyMap()
        _searchQuery.value = ""
        _selectedCircle.value = null
        _selectedAuthor.value = null
        _selectedTag.value = null
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
            _items.value = (_items.value + newItems).distinctBy { it.path }

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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCircle(circle: String?) { _selectedCircle.value = circle }
    fun selectAuthor(author: String?) { _selectedAuthor.value = author }
    fun selectTag(tag: String?) { _selectedTag.value = tag }

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

    /** RJコードから詳細メタデータを非同期で取得してキャッシュを更新する */
    fun fetchMetadataForRJItems() {
        val rjItems = _items.value.mapNotNull { item ->
            val match = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(item.name)
            match?.value?.uppercase()
        }.distinct()

        viewModelScope.launch {
            rjItems.forEach { rj ->
                if (!_metadataCache.value.containsKey(rj)) {
                    val info = folderRepository.getDlSiteMetadata(rj)
                    if (info != null) {
                        _metadataCache.value = _metadataCache.value + (rj to info)
                    }
                }
            }
        }
    }
}
