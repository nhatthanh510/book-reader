package com.example.docsach.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docsach.R
import com.example.docsach.ui.UiState
import com.example.docsach.ui.components.BookGridItem
import com.example.docsach.ui.components.DocSachTopAppBar
import com.example.docsach.ui.components.EmptyState
import com.example.docsach.ui.components.ErrorState
import com.example.docsach.ui.components.LoadingState

@Composable
fun CategoryBooksScreen(
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QueryBooksViewModel = viewModel(factory = QueryBooksViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // Fetch the next page once the last row is close, rather than waiting for the exact end.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { shouldLoadMore }.collect { if (it) viewModel.loadMore() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DocSachTopAppBar(
                title = uiState.query?.labelRes?.let { stringResource(it) }
                    ?: stringResource(R.string.app_name),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        when (val state = uiState.books) {
            UiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is UiState.Error -> ErrorState(
                messageRes = state.messageRes,
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )
            is UiState.Success -> if (state.data.isEmpty()) {
                EmptyState(R.string.empty_result, modifier = Modifier.padding(innerPadding))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.data, key = { it.id }) { book ->
                        BookGridItem(book = book, onClick = { onBookClick(book.id) })
                    }
                }
            }
        }
    }
}
