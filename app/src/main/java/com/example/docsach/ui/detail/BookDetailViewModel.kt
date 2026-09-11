package com.example.docsach.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.docsach.R
import com.example.docsach.data.BookRepository
import com.example.docsach.data.model.Book
import com.example.docsach.data.toMessageRes
import com.example.docsach.ui.UiState
import com.example.docsach.ui.bookRepository
import com.example.docsach.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val volumeId: String = savedStateHandle[Routes.ARG_VOLUME_ID] ?: ""

    private val _book = MutableStateFlow<UiState<Book>>(UiState.Loading)
    val book: StateFlow<UiState<Book>> = _book.asStateFlow()

    private val _related = MutableStateFlow<List<Book>>(emptyList())
    val related: StateFlow<List<Book>> = _related.asStateFlow()

    /** One-shot messages for the snackbar (saved / removed). */
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    val isFavorite: StateFlow<Boolean> = repository.observeIsFavorite(volumeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun load() {
        if (volumeId.isBlank()) {
            _book.value = UiState.Error(R.string.error_generic)
            return
        }
        viewModelScope.launch {
            _book.value = UiState.Loading
            repository.getBook(volumeId)
                .onSuccess { book ->
                    _book.value = UiState.Success(book)
                    // Related books are decoration: a failure must not break the detail page.
                    repository.getRelatedBooks(book).onSuccess { _related.value = it }
                }
                .onFailure { _book.value = UiState.Error(it.toMessageRes()) }
        }
    }

    fun toggleFavorite() {
        val book = (_book.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val added = repository.toggleFavorite(book)
            _message.value = if (added) R.string.saved_to_shelf else R.string.removed_from_shelf
        }
    }

    fun saveToShelf() {
        val book = (_book.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.saveToShelf(book)
            _message.value = R.string.saved_to_shelf
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BookDetailViewModel(this.bookRepository, this.createSavedStateHandle()) }
        }
    }
}
