package com.example.docsach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.docsach.data.BookRepository
import com.example.docsach.data.model.Book
import com.example.docsach.data.model.BookQuery
import com.example.docsach.data.model.homeSections
import com.example.docsach.data.toMessageRes
import com.example.docsach.ui.UiState
import com.example.docsach.ui.bookRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeSectionState(
    val query: BookQuery,
    val books: UiState<List<Book>>,
)

class HomeViewModel(private val repository: BookRepository) : ViewModel() {

    private val _sections = MutableStateFlow(
        homeSections.map { HomeSectionState(it, UiState.Loading) }
    )
    val sections: StateFlow<List<HomeSectionState>> = _sections.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _sections.value = homeSections.map { HomeSectionState(it, UiState.Loading) }
            // Load all rows at once; one failing row must not blank the whole page.
            val results = homeSections
                .map { section -> async { section to repository.searchBooks(section, pageSize = 12) } }
                .awaitAll()
            _sections.value = results.map { (section, result) ->
                HomeSectionState(
                    query = section,
                    books = result.fold(
                        onSuccess = { UiState.Success(it) },
                        onFailure = { UiState.Error(it.toMessageRes()) },
                    ),
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(this.bookRepository) }
        }
    }
}
