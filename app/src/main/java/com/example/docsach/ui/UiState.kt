package com.example.docsach.ui

import androidx.annotation.StringRes

/** One shape for every screen: loading, loaded, or a translated error. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(@param:StringRes val messageRes: Int) : UiState<Nothing>
}
