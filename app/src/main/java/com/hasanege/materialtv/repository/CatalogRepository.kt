package com.hasanege.materialtv.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.hasanege.materialtv.data.dao.CategoryDao
import com.hasanege.materialtv.data.dao.ContentDao
import com.hasanege.materialtv.data.entities.CategoryEntity
import com.hasanege.materialtv.data.entities.ContentEntity
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.sync.CatalogSyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val contentDao: ContentDao,
    private val syncManager: CatalogSyncManager
) {
    fun observeCategories(type: ContentType, profileId: String): Flow<List<CategoryEntity>> {
        return categoryDao.observeByType(type, profileId)
    }

    fun observeContents(categoryId: String?, type: ContentType, profileId: String): Flow<PagingData<ContentEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { contentDao.pagingSource(categoryId, type, profileId) }
        ).flow
    }

    fun observeAllContents(type: ContentType, profileId: String): Flow<List<ContentEntity>> {
        return contentDao.observeAll(type, profileId)
    }

    suspend fun triggerBackgroundSync(profileId: String, username: String, password: String, forceSync: Boolean = false) {
        syncManager.syncIfNeeded(profileId, username, password, forceSync)
    }
}
