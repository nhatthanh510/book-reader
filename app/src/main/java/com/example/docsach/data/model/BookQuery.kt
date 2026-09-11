package com.example.docsach.data.model

import androidx.annotation.StringRes
import com.example.docsach.R

/**
 * A named Google Books search. The home sections and the "Danh mục" tab are both just queries,
 * so they share one type and one screen for the "Xem thêm" grid.
 */
data class BookQuery(
    val key: String,
    @param:StringRes val labelRes: Int,
    val q: String,
    val orderBy: String? = null,
)

/** The three horizontal rows on the home screen, in mockup order. */
val homeSections: List<BookQuery> = listOf(
    BookQuery("newest", R.string.section_newest, q = "truyện", orderBy = "newest"),
    BookQuery("popular", R.string.section_popular, q = "sách hay", orderBy = "relevance"),
    BookQuery("featured", R.string.section_featured, q = "kỹ năng sống", orderBy = "relevance"),
)

/**
 * Genres for the "Danh mục" tab. Plain Vietnamese keywords beat BISAC `subject:` values here,
 * because Google's subject taxonomy is English-only and returns almost nothing under langRestrict=vi.
 */
val bookCategories: List<BookQuery> = listOf(
    BookQuery("literature", R.string.category_literature, q = "văn học"),
    BookQuery("economics", R.string.category_economics, q = "kinh tế"),
    BookQuery("life_skills", R.string.category_life_skills, q = "kỹ năng sống"),
    BookQuery("children", R.string.category_children, q = "truyện thiếu nhi"),
    BookQuery("comics", R.string.category_comics, q = "truyện tranh"),
    BookQuery("history", R.string.category_history, q = "lịch sử"),
    BookQuery("science", R.string.category_science, q = "khoa học"),
    BookQuery("psychology", R.string.category_psychology, q = "tâm lý"),
)

fun findQueryByKey(key: String): BookQuery? =
    (homeSections + bookCategories).firstOrNull { it.key == key }
