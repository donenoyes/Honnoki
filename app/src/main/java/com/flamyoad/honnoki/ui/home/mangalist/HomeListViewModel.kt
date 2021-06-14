package com.flamyoad.honnoki.ui.home.mangalist

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.flamyoad.honnoki.data.db.AppDatabase
import com.flamyoad.honnoki.data.model.Manga
import com.flamyoad.honnoki.data.model.Source
import com.flamyoad.honnoki.source.BaseSource
import com.flamyoad.honnoki.source.MangakalotSource
import com.flamyoad.honnoki.source.SenMangaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

@ExperimentalPagingApi
class HomeListViewModel(val db: AppDatabase, val mangaSource: BaseSource) : ViewModel() {

    fun getRecentManga(): Flow<PagingData<Manga>> = mangaSource.getRecentManga()
        .flowOn(Dispatchers.IO)
        .cachedIn(viewModelScope)

    fun getTrendingManga(): Flow<PagingData<Manga>> = mangaSource.getTrendingManga()
        .flowOn(Dispatchers.IO)
        .cachedIn(viewModelScope)

    fun getNewManga(): Flow<PagingData<Manga>> = mangaSource.getNewManga()
        .flowOn(Dispatchers.IO)
        .cachedIn(viewModelScope)
}