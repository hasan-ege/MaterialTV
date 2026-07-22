package com.hasanege.materialtv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        com.hasanege.materialtv.data.entities.WatchHistoryEntity::class
    ],
    version = 7, // Incremented for new offline-first schema
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
                
                val factory = net.sqlcipher.database.SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
