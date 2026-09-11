package com.example.docsach.data.remote

import com.example.docsach.data.model.Book
import com.example.docsach.data.remote.dto.VolumeDto
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Google Books -> domain conversion. Kept free of Android APIs so it can be unit tested on the JVM,
 * which is why HTML stripping is hand-rolled instead of using HtmlCompat.
 */
object BookMapper {

    fun VolumeDto.toBook(): Book {
        val info = volumeInfo
        val access = accessInfo
        val price = saleInfo?.retailPrice ?: saleInfo?.listPrice
        return Book(
            id = id,
            title = info?.title.orEmpty().ifBlank { "—" },
            authors = info?.authors.orEmpty(),
            thumbnailUrl = sanitizeImageUrl(info?.imageLinks?.thumbnail ?: info?.imageLinks?.smallThumbnail),
            description = stripHtml(info?.description),
            categories = info?.categories.orEmpty(),
            priceAmount = price?.amount,
            priceCurrency = price?.currencyCode,
            webReaderLink = toHttps(access?.webReaderLink),
            previewLink = toHttps(info?.previewLink),
            embeddable = access?.embeddable == true,
            viewability = access?.viewability,
            publishedDate = info?.publishedDate,
            pageCount = info?.pageCount,
            averageRating = info?.averageRating,
        )
    }

    /**
     * Google hands out `http://` links throughout its payload — covers, preview links and the web
     * reader alike. Cleartext is blocked by default from API 28 up, so covers silently fail to load
     * and the reader WebView dies with ERR_CLEARTEXT_NOT_PERMITTED unless the scheme is upgraded.
     */
    fun toHttps(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
    }

    /** Cover URLs additionally carry a page-curl overlay that looks like a folded corner. */
    fun sanitizeImageUrl(url: String?): String? {
        val https = toHttps(url) ?: return null
        return https
            .replace("&edge=curl", "")
            .replace("?edge=curl", "")
    }

    /** Descriptions come back as small HTML fragments; the UI renders plain text. */
    fun stripHtml(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val text = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return text.ifBlank { null }
    }

    /**
     * Formats a sale price, or returns null when the volume has none so the caller can fall back to
     * the "0 đ" string the mockup shows.
     */
    fun formatPrice(amount: Double?, currencyCode: String?): String? {
        if (amount == null || amount <= 0.0) return null
        val currency = runCatching { Currency.getInstance(currencyCode ?: "VND") }.getOrNull()
            ?: return amount.toString()
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = currency
            maximumFractionDigits = if (currency.currencyCode == "VND") 0 else 2
        }
        return format.format(amount)
    }
}
