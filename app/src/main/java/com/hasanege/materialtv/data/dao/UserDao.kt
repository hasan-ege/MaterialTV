package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.data.entities.FavoriteEntity
import com.hasanege.materialtv.data.entities.FavoriteWithContent
import com.hasanege.materialtv.data.entities.ListFolderEntity
import com.hasanege.materialtv.data.entities.UserRatingEntity
import com.hasanege.materialtv.data.entities.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Favorites
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE streamId = :streamId AND type = :type AND profileId = :profileId")
    suspend fun deleteFavoriteByContent(streamId: String, type: ContentType, profileId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE streamId = :streamId AND type = :type AND profileId = :profileId)")
    suspend fun isFavorite(streamId: String, type: ContentType, profileId: String): Boolean

    @Query("SELECT * FROM favorites WHERE streamId = :streamId AND type = :type AND profileId = :profileId LIMIT 1")
    suspend fun getFavorite(streamId: String, type: ContentType, profileId: String): FavoriteEntity?

    @Query("SELECT f.*, c.name, c.posterUrl, c.genre, c.releaseDate, c.rating, c.categoryId FROM favorites f LEFT JOIN contents c ON f.streamId = c.streamId AND f.type = c.type AND f.profileId = c.profileId WHERE f.profileId = :profileId ORDER BY f.addedAt DESC")
    fun observeFavoritesWithContent(profileId: String): Flow<List<FavoriteWithContent>>

    @Query("SELECT f.*, c.name, c.posterUrl, c.genre, c.releaseDate, c.rating, c.categoryId FROM favorites f LEFT JOIN contents c ON f.streamId = c.streamId AND f.type = c.type AND f.profileId = c.profileId WHERE f.profileId = :profileId AND f.folderId = :folderId ORDER BY f.addedAt DESC")
    fun observeFavoritesByFolderWithContent(profileId: String, folderId: String): Flow<List<FavoriteWithContent>>

    @Query("SELECT f.*, c.name, c.posterUrl, c.genre, c.releaseDate, c.rating, c.categoryId FROM favorites f LEFT JOIN contents c ON f.streamId = c.streamId AND f.type = c.type AND f.profileId = c.profileId WHERE f.profileId = :profileId AND f.type = :type ORDER BY f.addedAt DESC")
    fun observeFavoritesByTypeWithContent(profileId: String, type: ContentType): Flow<List<FavoriteWithContent>>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun observeFavorites(profileId: String): Flow<List<FavoriteEntity>>


    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND folderId = :folderId ORDER BY addedAt DESC")
    fun observeFavoritesByFolder(profileId: String, folderId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND type = :type ORDER BY addedAt DESC")
    fun observeFavoritesByType(profileId: String, type: ContentType): Flow<List<FavoriteEntity>>
    
    @Query("SELECT COUNT(*) FROM favorites WHERE profileId = :profileId AND folderId = :folderId")
    suspend fun getFavoritesCountByFolder(profileId: String, folderId: String): Int

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND folderId = :folderId")
    suspend fun deleteAllFavoritesByFolder(profileId: String, folderId: String)

    // List Folders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ListFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: ListFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: ListFolderEntity)

    @Query("SELECT * FROM list_folders WHERE profileId = :profileId ORDER BY sortOrder ASC")
    fun observeFolders(profileId: String): Flow<List<ListFolderEntity>>
    
    @Query("SELECT * FROM list_folders WHERE profileId = :profileId AND folderId = :folderId LIMIT 1")
    suspend fun getFolderById(profileId: String, folderId: String): ListFolderEntity?

    @Query("UPDATE list_folders SET sortOrder = :newIndex WHERE folderId = :folderId AND profileId = :profileId")
    suspend fun updateFolderOrder(folderId: String, profileId: String, newIndex: Int)
}
