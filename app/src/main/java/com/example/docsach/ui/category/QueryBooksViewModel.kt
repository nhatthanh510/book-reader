package com.example.docsach.ui.category

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
import com.example.docsach.data.model.BookQuery
import com.example.docsach.data.model.findQueryByKey
import com.example.docsach.data.toMessageRes
import com.example.docsach.ui.UiState
import com.example.docsach.ui.bookRepository
import com.example.docsach.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueryBooksUiState(
    val query: BookQuery?,
    val books: UiState<List<Book>> = UiState.Loading,
    val isAppending: Boolean = false,
    val endReached: Boolean = false,
)

/** Backs the "Xem thêm" grid for a home section and the grid for a genre — both are one query. */
class QueryBooksViewModel(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val queryKey: String = savedStateHandle[Routes.ARG_QUERY_KEY] ?: ""
    private val query: BookQuery? = findQueryByKey(queryKey)

    private val _uiState = MutableStateFlow(QueryBooksUiState(query = query))
    val uiState: StateFlow<QueryBooksUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val query = query ?: run {
            _uiState.update { it.copy(books = UiState.Error(R.string.error_generic)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(books = UiState.Loading, endReached = false) }
            repository.searchBooks(query, startIndex = 0)
                .onSuccess { books ->
                    _uiState.update {
                        it.copy(books = UiState.Success(books), endReached = books.isEmpty())
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(books = UiState.Error(error.toMessageRes())) }
                }
        }
    }

    /** Called when the grid scrolls near its end. */
    fun loadMore() {
        val query = query ?: return
        val current = _uiState.value
        val loaded = (current.books as? UiState.Success)?.data ?: return
        if (current.isAppending || current.endReached) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAppending = true) }
            repository.searchBooks(query, startIndex = loaded.size)
                .onSuccess { more ->
                    // Google can repeat volumes across pages; de-duplicate or LazyColumn keys crash.
                    val merged = (loaded + more).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            books = UiState.Success(merged),
                            isAppending = false,
                            endReached = more.isEmpty() || merged.size == loaded.size,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isAppending = false, endReached = true) }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { QueryBooksViewModel(this.bookRepository, this.createSavedStateHandle()) }
        }
    }
}
