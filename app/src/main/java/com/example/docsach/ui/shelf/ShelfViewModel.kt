package com.example.docsach.ui.shelf

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.docsach.R
import com.example.docsach.data.BookRepository
import com.example.docsach.data.ShelfFilter
import com.example.docsach.data.model.Book
import com.example.docsach.ui.bookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@get:StringRes
val ShelfFilter.labelRes: Int
    get() = when (this) {
        ShelfFilter.ALL -> R.string.shelf_filter_all
        ShelfFilter.FAVORITE -> R.string.shelf_filter_favorite
        ShelfFilter.READING -> R.string.shelf_filter_reading
    }

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfViewModel(private val repository: BookRepository) : ViewModel() {

    private val _filter = MutableStateFlow(ShelfFilter.ALL)
    val filter: StateFlow<ShelfFilter> = _filter.asStateFlow()

    val books: StateFlow<List<Book>> = _filter
        .flatMapLatest { repository.observeShelf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: ShelfFilter) {
        _filter.value = filter
    }

    fun remove(bookId: String) {
        viewModelScope.launch { repository.removeFromShelf(bookId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ShelfViewModel(this.bookRepository) }
        }
    }
}
