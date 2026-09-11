package com.example.docsach.data.model

/**
 * A book as the UI needs it. Deliberately flat: the same shape comes back from the Google Books
 * API and from Room, so screens never care which side of the repository served them.
 */
data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val thumbnailUrl: String?,
    val description: String?,
    val categories: List<String>,
    /** `null` means Google reported no sale price, which the UI renders as "0 đ". */
    val priceAmount: Double?,
    val priceCurrency: String?,
    val webReaderLink: String?,
    val previewLink: String?,
    val embeddable: Boolean,
    /** NO_PAGES / PARTIAL / ALL_PAGES — decides whether the reader can embed Google's viewer. */
    val viewability: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val averageRating: Double?,
) {
    val authorText: String get() = authors.joinToString(", ")

    /**
     * URL the reader loads. previewLink comes first on purpose: the Play Books reader behind
     * webReaderLink refuses to render for a signed-out user ("unviewable manifest"), while the
     * classic books.google.com preview page works anonymously.
     */
    val readerUrl: String?
        get() = previewLink?.takeIf { it.isNotBlank() } ?: webReaderLink?.takeIf { it.isNotBlank() }

    /** True when Google will let us show actual book pages inside a WebView. */
    val hasReadablePreview: Boolean
        get() = embeddable && readerUrl != null && viewability != "NO_PAGES"
}
