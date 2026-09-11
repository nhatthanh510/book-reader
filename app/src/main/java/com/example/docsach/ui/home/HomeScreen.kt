package com.example.docsach.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docsach.R
import com.example.docsach.ui.UiState
import com.example.docsach.ui.components.BookCard
import com.example.docsach.ui.components.DocSachTopAppBar
import com.example.docsach.ui.components.ErrorState
import com.example.docsach.ui.components.LoadingState
import com.example.docsach.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (String) -> Unit,
    onSeeMore: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            DocSachTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(sections, key = { it.query.key }) { section ->
                Column {
                    SectionHeader(
                        title = stringResource(section.query.labelRes),
                        onSeeMore = { onSeeMore(section.query.key) },
                    )
                    when (val state = section.books) {
                        UiState.Loading -> LoadingState(Modifier.height(200.dp))
                        is UiState.Error -> ErrorState(state.messageRes, onRetry = viewModel::load)
                        is UiState.Success -> LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.data, key = { it.id }) { book ->
                                BookCard(book = book, onClick = { onBookClick(book.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
