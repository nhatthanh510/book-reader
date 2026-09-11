package com.example.docsach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.docsach.data.model.Book
import com.example.docsach.data.remote.BookMapper

/**
 * A book the user put on their shelf. It stores every field the UI needs, so the shelf — and a book
 * detail page reached from it — keep working with no network.
 */
@Entity(tableName = "saved_books")
data class SavedBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authors: String,
    val thumbnailUrl: String?,
    val description: String?,
    val categories: String,
    val priceAmount: Double?,
    val priceCurrency: String?,
    val webReaderLink: String?,
    val previewLink: String?,
    val embeddable: Boolean,
    val viewability: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val averageRating: Double?,
    val isFavorite: Boolean,
    val savedAt: Long,
)

private const val LIST_SEPARATOR = "|"

fun SavedBookEntity.toBook(): Book = Book(
    id = id,
    title = title,
    authors = authors.split(LIST_SEPARATOR).filter { it.isNotBlank() },
    thumbnailUrl = thumbnailUrl,
    description = description,
    categories = categories.split(LIST_SEPARATOR).filter { it.isNotBlank() },
    priceAmount = priceAmount,
    priceCurrency = priceCurrency,
    // Defensive: rows saved by an earlier build may still carry cleartext http links.
    webReaderLink = BookMapper.toHttps(webReaderLink),
    previewLink = BookMapper.toHttps(previewLink),
    embeddable = embeddable,
    viewability = viewability,
    publishedDate = publishedDate,
    pageCount = pageCount,
    averageRating = averageRating,
)

fun Book.toSavedEntity(isFavorite: Boolean, savedAt: Long = System.currentTimeMillis()): SavedBookEntity =
    SavedBookEntity(
        id = id,
        title = title,
        authors = authors.joinToString(LIST_SEPARATOR),
        thumbnailUrl = thumbnailUrl,
        description = description,
        categories = categories.joinToString(LIST_SEPARATOR),
        priceAmount = priceAmount,
        priceCurrency = priceCurrency,
        webReaderLink = webReaderLink,
        previewLink = previewLink,
        embeddable = embeddable,
        viewability = viewability,
        publishedDate = publishedDate,
        pageCount = pageCount,
        averageRating = averageRating,
        isFavorite = isFavorite,
        savedAt = savedAt,
    )
