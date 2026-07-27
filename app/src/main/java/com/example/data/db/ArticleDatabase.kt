package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarked_articles")
data class BookmarkedArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val extract: String,
    val thumbnailUrl: String?,
    val lang: String,
    val topic: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(article: BookmarkedArticle)

    @Query("DELETE FROM bookmarked_articles WHERE title = :title")
    suspend fun deleteBookmarkByTitle(title: String)

    @Query("SELECT * FROM bookmarked_articles ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedArticle>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_articles WHERE title = :title)")
    fun isBookmarked(title: String): Flow<Boolean>

    @Query("DELETE FROM bookmarked_articles")
    suspend fun clearAllBookmarks()
}

@Database(entities = [BookmarkedArticle::class], version = 1, exportSchema = false)
abstract class ScrollBreakDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: ScrollBreakDatabase? = null

        fun getDatabase(context: Context): ScrollBreakDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollBreakDatabase::class.java,
                    "scrollbreak_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
