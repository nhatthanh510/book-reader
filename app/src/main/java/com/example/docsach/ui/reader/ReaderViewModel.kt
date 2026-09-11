package com.example.docsach.ui.reader

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val book: UiState<Book> = UiState.Loading,
    val fontScalePercent: Int = 100,
    val restoredScrollOffset: Int = 0,
)

class ReaderViewModel(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val volumeId: String = savedStateHandle[Routes.ARG_VOLUME_ID] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (volumeId.isBlank()) {
            _uiState.update { it.copy(book = UiState.Error(R.string.error_generic)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(book = UiState.Loading) }
            repository.getBook(volumeId)
                .onSuccess { book ->
                    val progress = repository.getProgress(book.id)
                    _uiState.update {
                        it.copy(
                            book = UiState.Success(book),
                            fontScalePercent = progress?.fontScalePercent ?: 100,
                            restoredScrollOffset = progress?.scrollOffset ?: 0,
                        )
                    }
                    // Opening the reader is what marks a book "Đang đọc" on the shelf.
                    repository.saveProgress(book, progress?.scrollOffset ?: 0, progress?.fontScalePercent ?: 100)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(book = UiState.Error(error.toMessageRes())) }
                }
        }
    }

    fun changeFontScale(delta: Int) {
        _uiState.update {
            it.copy(fontScalePercent = (it.fontScalePercent + delta).coerceIn(MIN_SCALE, MAX_SCALE))
        }
    }

    /** Called when the reader leaves the screen, so the next visit resumes where it stopped. */
    fun persistProgress(scrollOffset: Int) {
        val book = (_uiState.value.book as? UiState.Success)?.data ?: return
        val fontScale = _uiState.value.fontScalePercent
        viewModelScope.launch {
            repository.saveProgress(book, scrollOffset, fontScale)
        }
    }

    companion object {
        const val MIN_SCALE = 80
        const val MAX_SCALE = 200
        const val SCALE_STEP = 10

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ReaderViewModel(this.bookRepository, this.createSavedStateHandle()) }
        }
    }
}
