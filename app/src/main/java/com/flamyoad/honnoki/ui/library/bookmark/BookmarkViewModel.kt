package com.flamyoad.honnoki.ui.library.bookmark

import android.app.Application
import androidx.lifecycle.*
import androidx.room.withTransaction
import com.flamyoad.honnoki.data.db.AppDatabase
import com.flamyoad.honnoki.data.model.BookmarkGroup
import com.flamyoad.honnoki.data.model.BookmarkGroupWithInfo
import com.flamyoad.honnoki.data.model.BookmarkWithOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val bookmarkGroupDao = db.bookmarkGroupDao()

    private val selectedBookmarkGroupId = MutableStateFlow(-1L)

    val bookmarkGroupsWithInfo: LiveData<List<BookmarkGroupWithInfo>> =
        db.bookmarkGroupWithInfoDao()
            .getAll()
            .flatMapLatest { flowOf(attachCoverImagesToGroups(it)) }
            .asLiveData()

    var bookmarkGroupName: String = ""
        private set

    val selectedBookmarkGroup = selectedBookmarkGroupId
        .onEach { flowOf(BookmarkGroup.empty()) }
        .filter { it != -1L }
        .flatMapLatest { return@flatMapLatest db.bookmarkGroupDao().getById(it) }

    val bookmarkItems: LiveData<List<BookmarkWithOverview>> = selectedBookmarkGroup
        .onEach { flowOf(emptyList<BookmarkWithOverview>()) }
        .filterNotNull()
        .flatMapLatest { db.bookmarkDao().getAllWithOverviewFrom(it.id ?: -1L) }
        .asLiveData()

    init {
        initializeBookmarkGroups()
    }

    private fun initializeBookmarkGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedBookmarkGroupId.value = bookmarkGroupDao.getFirstItemId()
        }
    }

    private suspend fun attachCoverImagesToGroups(groups: List<BookmarkGroupWithInfo>): List<BookmarkGroupWithInfo> {
        return withContext(Dispatchers.IO) {
            groups.map {
                val bookmarkGroupId = it.bookmarkGroup.id ?: -1
                val coverImages = db.bookmarkGroupDao().getCoverImagesFrom(bookmarkGroupId)
                return@map it.copy(coverImages = coverImages)
            }
        }
    }

    fun getSelectedBookmarkGroupId(): Long {
        return requireNotNull(selectedBookmarkGroupId.value)
    }

    fun selectBookmarkGroup(bookmarkGroup: BookmarkGroup) {
        selectedBookmarkGroupId.value = bookmarkGroup.id ?: -1
    }
}