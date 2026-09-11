package com.example.docsach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBookDao {

    @Query("SELECT * FROM saved_books ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedBookEntity>>

    @Query("SELECT * FROM saved_books WHERE isFavorite = 1 ORDER BY savedAt DESC")
    fun observeFavorites(): Flow<List<SavedBookEntity>>

    /** Shelf entries that have a reading-progress row, newest first. */
    @Query(
        """
        SELECT b.* FROM saved_books AS b
        INNER JOIN reading_progress AS p ON p.bookId = b.id
        ORDER BY p.lastReadAt DESC
        """
    )
    fun observeReading(): Flow<List<SavedBookEntity>>

    @Query("SELECT * FROM saved_books WHERE id = :bookId LIMIT 1")
    suspend fun findById(bookId: String): SavedBookEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_books WHERE id = :bookId AND isFavorite = 1)")
    fun observeIsFavorite(bookId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: SavedBookEntity)

    @Query("DELETE FROM saved_books WHERE id = :bookId")
    suspend fun deleteById(bookId: String)
}

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun findById(bookId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteById(bookId: String)
}
