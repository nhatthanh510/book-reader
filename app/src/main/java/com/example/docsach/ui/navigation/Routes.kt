package com.example.docsach.ui.navigation

object Routes {
    const val HOME = "home"
    const val SHELF = "shelf"
    const val CATEGORIES = "categories"
    const val SEARCH = "search"

    const val ARG_QUERY_KEY = "queryKey"
    const val ARG_VOLUME_ID = "volumeId"

    /** Grid for one home section or one genre; carries the key of a known BookQuery. */
    const val QUERY_BOOKS = "query/{$ARG_QUERY_KEY}"
    const val DETAIL = "detail/{$ARG_VOLUME_ID}"
    const val READER = "reader/{$ARG_VOLUME_ID}"

    fun queryBooks(queryKey: String) = "query/$queryKey"
    fun detail(volumeId: String) = "detail/$volumeId"
    fun reader(volumeId: String) = "reader/$volumeId"

    /** Destinations that keep the bottom bar visible. */
    val topLevel = setOf(HOME, SHELF, CATEGORIES)
}
