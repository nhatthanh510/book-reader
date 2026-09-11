package com.example.docsach.data

import com.example.docsach.R
import com.example.docsach.data.local.ReadingProgressDao
import com.example.docsach.data.local.ReadingProgressEntity
import com.example.docsach.data.local.SavedBookDao
import com.example.docsach.data.local.toBook
import com.example.docsach.data.local.toSavedEntity
import com.example.docsach.data.model.Book
import com.example.docsach.data.model.BookQuery
import com.example.docsach.data.remote.BookMapper.toBook
import com.example.docsach.data.remote.GoogleBooksApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Filters offered by the dropdown on the shelf screen. */
enum class ShelfFilter { ALL, FAVORITE, READING }

class BookRepository(
    private val api: GoogleBooksApi,
    private val savedBookDao: SavedBookDao,
    private val progressDao: ReadingProgressDao,
    private val apiKey: String,
) {

    private val hasApiKey: Boolean get() = apiKey.isNotBlank()

    suspend fun searchBooks(
        query: BookQuery,
        startIndex: Int = 0,
        pageSize: Int = GoogleBooksApi.DEFAULT_PAGE_SIZE,
    ): Result<List<Book>> = searchBooks(query.q, query.orderBy, startIndex, pageSize)

    suspend fun searchBooks(
        q: String,
        orderBy: String? = null,
        startIndex: Int = 0,
        pageSize: Int = GoogleBooksApi.DEFAULT_PAGE_SIZE,
        langRestrict: String? = VIETNAMESE,
    ): Result<List<Book>> = apiCall {
        val response = api.searchVolumes(
            query = q,
            orderBy = orderBy,
            langRestrict = langRestrict,
            startIndex = startIndex,
            maxResults = pageSize,
        )
        val books = response.items.orEmpty().map { it.toBook() }
        // Vietnamese-restricted queries go thin for some genres; widen rather than show an empty grid.
        if (books.isEmpty() && langRestrict != null && startIndex == 0) {
            api.searchVolumes(q, orderBy, null, startIndex, pageSize).items.orEmpty().map { it.toBook() }
        } else {
            books
        }
    }

    /** Detail page: prefer the live volume, fall back to the shelf copy when offline. */
    suspend fun getBook(volumeId: String): Result<Book> {
        val cached = savedBookDao.findById(volumeId)?.toBook()
        if (!hasApiKey) {
            return cached?.let { Result.success(it) } ?: Result.failure(MissingApiKeyException())
        }
        return runCatching { api.getVolume(volumeId).toBook() }
            .recoverCatching { error -> cached ?: throw error }
    }

    suspend fun getRelatedBooks(book: Book): Result<List<Book>> {
        val author = book.authors.firstOrNull()
        val q = when {
            !author.isNullOrBlank() -> "inauthor:\"$author\""
            book.categories.isNotEmpty() -> book.categories.first()
            else -> book.title
        }
        return searchBooks(q = q, pageSize = 12, langRestrict = null)
            .map { books -> books.filter { it.id != book.id } }
    }

    // ---- Shelf -------------------------------------------------------------------------------

    fun observeShelf(filter: ShelfFilter): Flow<List<Book>> {
        val source = when (filter) {
            ShelfFilter.ALL -> savedBookDao.observeAll()
            ShelfFilter.FAVORITE -> savedBookDao.observeFavorites()
            ShelfFilter.READING -> savedBookDao.observeReading()
        }
        return source.map { entities -> entities.map { it.toBook() } }
    }

    fun observeIsFavorite(bookId: String): Flow<Boolean> = savedBookDao.observeIsFavorite(bookId)

    /** Toggles the heart. Returns true when the book ended up on the shelf. */
    suspend fun toggleFavorite(book: Book): Boolean {
        val existing = savedBookDao.findById(book.id)
        return if (existing != null && existing.isFavorite) {
            savedBookDao.deleteById(book.id)
            progressDao.deleteById(book.id)
            false
        } else {
            savedBookDao.upsert(book.toSavedEntity(isFavorite = true))
            true
        }
    }

    /** The download action: keep a copy on the shelf without marking it a favourite. */
    suspend fun saveToShelf(book: Book) {
        val existing = savedBookDao.findById(book.id)
        savedBookDao.upsert(book.toSavedEntity(isFavorite = existing?.isFavorite == true))
    }

    suspend fun removeFromShelf(bookId: String) {
        savedBookDao.deleteById(bookId)
        progressDao.deleteById(bookId)
    }

    // ---- Reading progress --------------------------------------------------------------------

    suspend fun getProgress(bookId: String): ReadingProgressEntity? = progressDao.findById(bookId)

    /** Called when the reader opens and when it closes, so "Đang đọc" reflects real activity. */
    suspend fun saveProgress(book: Book, scrollOffset: Int, fontScalePercent: Int) {
        // A progress row is meaningless without its book, and observeReading() joins on saved_books.
        if (savedBookDao.findById(book.id) == null) {
            savedBookDao.upsert(book.toSavedEntity(isFavorite = false))
        }
        progressDao.upsert(
            ReadingProgressEntity(
                bookId = book.id,
                lastReadAt = System.currentTimeMillis(),
                scrollOffset = scrollOffset,
                fontScalePercent = fontScalePercent,
            )
        )
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> {
        if (!hasApiKey) return Result.failure(MissingApiKeyException())
        return runCatching { block() }
    }

    private companion object {
        const val VIETNAMESE = "vi"
    }
}

class MissingApiKeyException : IllegalStateException("GOOGLE_BOOKS_API_KEY is not configured")

/** Maps a repository failure to a user-facing string resource. */
fun Throwable.toMessageRes(): Int = when (this) {
    is MissingApiKeyException -> R.string.error_no_api_key
    is IOException -> R.string.error_network
    else -> R.string.error_generic
}
