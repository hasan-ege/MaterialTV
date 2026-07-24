package com.hasanege.materialtv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DownloadEntity::class,
        com.hasanege.materialtv.data.entities.CategoryEntity::class,
        com.hasanege.materialtv.data.entities.ContentEntity::class,
        com.hasanege.materialtv.data.entities.CastEntity::class,
        com.hasanege.materialtv.data.entities.ContentImageEntity::class,
        com.hasanege.materialtv.data.entities.EpisodeEntity::class,
        com.hasanege.materialtv.data.entities.SyncMetaEntity::class,
        com.hasanege.materialtv.data.entities.FavoriteEntity::class,
        com.hasanege.materialtv.data.entities.ListFolderEntity::class,
        com.hasanege.materialtv.data.entities.UserRatingEntity::class,
        com.hasanege.materialtv.data.entities.WatchHistoryEntity::class,
        com.hasanege.materialtv.data.entities.TmdbContentEntity::class
    ],
    version = 13, // Incremented for SkipDB downloads support
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun categoryDao(): com.hasanege.materialtv.data.dao.CategoryDao
    abstract fun contentDao(): com.hasanege.materialtv.data.dao.ContentDao
    abstract fun castDao(): com.hasanege.materialtv.data.dao.CastDao
    abstract fun episodeDao(): com.hasanege.materialtv.data.dao.EpisodeDao
    abstract fun syncMetaDao(): com.hasanege.materialtv.data.dao.SyncMetaDao
    abstract fun userDao(): com.hasanege.materialtv.data.dao.UserDao
    abstract fun tmdbDao(): com.hasanege.materialtv.data.dao.TmdbDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getOrCreatePassphrase(context: Context): ByteArray {
            return try {
                val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                val sharedPreferences = androidx.security.crypto.EncryptedSharedPreferences.create(
                    "database_security_prefs",
                    masterKeyAlias,
                    context,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                val key = "database_passphrase"
                var passphrase = sharedPreferences.getString(key, null)
                if (passphrase == null) {
                    val random = java.security.SecureRandom()
                    val bytes = ByteArray(32)
                    random.nextBytes(bytes)
                    passphrase = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    sharedPreferences.edit().putString(key, passphrase).apply()
                }
                android.util.Base64.decode(passphrase, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
                "materialtv_fallback_secure_key_12345678".toByteArray(Charsets.UTF_8)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbName = "materialtv_database"
                val dbFile = context.getDatabasePath(dbName)
                val passphrase = getOrCreatePassphrase(context)
                
                if (dbFile.exists()) {
                    try {
                        net.sqlcipher.database.SQLiteDatabase.loadLibs(context.applicationContext)
                        val db = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                            dbFile.absolutePath,
                            passphrase,
                            null,
                            net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY,
                            null,
                            null
                        )
                        db.close()
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "Database verification failed (likely legacy unencrypted or corrupted). Wiping database files...", e)
                        try {
                            context.deleteDatabase(dbName)
                            
                            // Manually clean up any lingering journal/wal files
                            val parentDir = dbFile.parentFile
                            if (parentDir != null) {
                                java.io.File(parentDir, "$dbName-journal").delete()
                                java.io.File(parentDir, "$dbName-shm").delete()
                                java.io.File(parentDir, "$dbName-wal").delete()
                            }
                        } catch (ex: Exception) {
                            android.util.Log.e("AppDatabase", "Error deleting legacy database files", ex)
                        }
                    }
                }
                
                val MIGRATION_8_9 = object : Migration(8, 9) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `tmdb_content` (
                                `streamId` TEXT NOT NULL,
                                `type` TEXT NOT NULL,
                                `profileId` TEXT NOT NULL,
                                `tmdbId` INTEGER NOT NULL,
                                `title` TEXT,
                                `overview` TEXT,
                                `posterPath` TEXT,
                                `backdropPath` TEXT,
                                `voteAverage` REAL,
                                `releaseDate` TEXT,
                                `fetchedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`streamId`, `type`, `profileId`),
                                FOREIGN KEY(`streamId`, `type`, `profileId`) REFERENCES `contents`(`streamId`, `type`, `profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        database.execSQL("CREATE INDEX IF NOT EXISTS `index_tmdb_content_streamId_type_profileId` ON `tmdb_content` (`streamId`, `type`, `profileId`)")
                    }
                }

                val MIGRATION_9_10 = object : Migration(9, 10) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE `tmdb_content` ADD COLUMN `director` TEXT")
                        database.execSQL("ALTER TABLE `tmdb_content` ADD COLUMN `directorAvatar` TEXT")
                        database.execSQL("ALTER TABLE `tmdb_content` ADD COLUMN `castJson` TEXT")
                    }
                }

                val MIGRATION_10_11 = object : Migration(10, 11) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE `tmdb_content` ADD COLUMN `imdbId` TEXT")
                    }
                }

                val MIGRATION_11_12 = object : Migration(11, 12) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Schema refresh
                    }
                }

                val MIGRATION_12_13 = object : Migration(12, 13) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE `downloads` ADD COLUMN `imdbId` TEXT NOT NULL DEFAULT ''")
                        database.execSQL("ALTER TABLE `downloads` ADD COLUMN `skipDbSegmentsJson` TEXT")
                    }
                }

                val factory = net.sqlcipher.database.SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
