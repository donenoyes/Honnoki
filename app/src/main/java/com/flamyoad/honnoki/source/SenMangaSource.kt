package com.flamyoad.honnoki.source

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.flamyoad.honnoki.api.SenMangaApi
import com.flamyoad.honnoki.data.GenreConstants
import com.flamyoad.honnoki.data.Source
import com.flamyoad.honnoki.data.State
import com.flamyoad.honnoki.data.entities.*
import com.flamyoad.honnoki.data.db.AppDatabase
import com.flamyoad.honnoki.paging.LookupSearchMediator
import com.flamyoad.honnoki.paging.MangaMediator
import com.flamyoad.honnoki.paging.SimpleSearchResultMediator
import com.flamyoad.honnoki.ui.lookup.model.LookupType
import kotlinx.coroutines.flow.Flow


@ExperimentalPagingApi
class SenMangaSource(db: AppDatabase, context: Context, private val api: SenMangaApi) :
    BaseSource(db, context) {

    override fun getSourceType(): Source {
        return Source.SENMANGA
    }

    override fun getRecentManga(): Flow<PagingData<Manga>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = true),
            remoteMediator = MangaMediator(api, db, getSourceType(), MangaType.RECENTLY),
            pagingSourceFactory = { db.mangaDao().getFrom(Source.SENMANGA, MangaType.RECENTLY) }
        ).flow
    }

    override fun getTrendingManga(): Flow<PagingData<Manga>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = true),
            remoteMediator = MangaMediator(api, db, getSourceType(), MangaType.TRENDING),
            pagingSourceFactory = { db.mangaDao().getFrom(Source.SENMANGA, MangaType.TRENDING) }
        ).flow
    }

    override suspend fun getChapterList(urlPath: String): State<List<Chapter>> {
        return api.searchForChapterList(urlPath)
    }

    override suspend fun getImages(urlPath: String): State<List<Page>> {
        return api.searchForImageList(urlPath)
    }

    override suspend fun getMangaOverview(urlPath: String): State<MangaOverview> {
        return api.searchForOverview(urlPath)
    }

    override suspend fun getAuthors(urlPath: String): State<List<Author>> {
        return api.searchForAuthors(urlPath)
    }

    override suspend fun getGenres(urlPath: String): State<List<Genre>> {
        return api.searchForGenres(urlPath)
    }

    override fun getSimpleSearch(query: String): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = false),
            remoteMediator = SimpleSearchResultMediator(
                api,
                db,
                query,
                GenreConstants.ALL
            ),
            pagingSourceFactory = { db.searchResultDao().getAll() }
        ).flow
    }

    override fun getSimpleSearchWithGenre(
        query: String,
        genre: GenreConstants
    ): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = false),
            remoteMediator = SimpleSearchResultMediator(
                api,
                db,
                query,
                genre
            ),
            pagingSourceFactory = { db.searchResultDao().getAll() }
        ).flow
    }

    override fun getMangaByAuthors(params: String): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = false),
            remoteMediator = LookupSearchMediator(
                api,
                db,
                params,
                LookupType.AUTHOR
            ),
            pagingSourceFactory = { db.searchResultDao().getAll() }
        ).flow
    }

    override fun getMangaByGenres(params: String): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGINATION_SIZE, enablePlaceholders = false),
            remoteMediator = LookupSearchMediator(
                api,
                db,
                params,
                LookupType.GENRE
            ),
            pagingSourceFactory = { db.searchResultDao().getAll() }
        ).flow
    }

    companion object {
        private const val PAGINATION_SIZE = 40
    }
}