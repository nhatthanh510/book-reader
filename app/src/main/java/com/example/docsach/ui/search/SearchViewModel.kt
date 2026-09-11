package com.example.docsach.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.docsach.data.BookRepository
import com.example.docsach.data.model.Book
import com.example.docsach.data.toMessageRes
import com.example.docsach.ui.UiState
import com.example.docsach.ui.bookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: BookRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null until the user actually submits something, which drives the "type a keyword" prompt. */
    private val _results = MutableStateFlow<UiState<List<Book>>?>(null)
    val results: StateFlow<UiState<List<Book>>?> = _results.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _results.value = UiState.Loading
            _results.value = repository.searchBooks(q = q, pageSize = 30).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toMessageRes()) },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(this.bookRepository) }
        }
    }
}
