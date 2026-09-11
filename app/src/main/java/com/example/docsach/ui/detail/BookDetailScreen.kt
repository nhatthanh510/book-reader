package com.example.docsach.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docsach.R
import com.example.docsach.data.model.Book
import com.example.docsach.data.remote.BookMapper
import com.example.docsach.ui.UiState
import com.example.docsach.ui.components.BookCard
import com.example.docsach.ui.components.BookCover
import com.example.docsach.ui.components.DocSachTopAppBar
import com.example.docsach.ui.components.ErrorState
import com.example.docsach.ui.components.LoadingState

@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onRead: (String) -> Unit,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel = viewModel(factory = BookDetailViewModel.Factory),
) {
    val bookState by viewModel.book.collectAsStateWithLifecycle()
    val related by viewModel.related.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DocSachTopAppBar(
                title = (bookState as? UiState.Success)?.data?.title
                    ?: stringResource(R.string.app_name),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::saveToShelf) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = stringResource(R.string.action_save_offline),
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.action_favorite_remove else R.string.action_favorite_add
                            ),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = bookState) {
            UiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is UiState.Error -> ErrorState(
                messageRes = state.messageRes,
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )
            is UiState.Success -> BookDetailContent(
                book = state.data,
                related = related,
                onRead = { onRead(state.data.id) },
                onBookClick = onBookClick,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    related: List<Book>,
    onRead: () -> Unit,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var descriptionExpanded by remember { mutableStateOf(false) }
    val price = BookMapper.formatPrice(book.priceAmount, book.priceCurrency)
        ?: stringResource(R.string.price_free)

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                BookCover(
                    url = book.thumbnailUrl,
                    title = book.title,
                    modifier = Modifier.width(120.dp).height(176.dp),
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(
                            R.string.detail_author,
                            book.authorText.ifBlank { stringResource(R.string.unknown_author) },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.detail_price, price),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    book.pageCount?.takeIf { it > 0 }?.let { pages ->
                        Text(
                            text = pluralStringResource(R.plurals.detail_pages, pages, pages),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Button(onClick = onRead, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.action_read))
                    }
                }
            }
            HorizontalDivider()
        }

        item {
            Text(
                text = stringResource(R.string.detail_intro),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                text = book.description ?: stringResource(R.string.detail_no_description),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (book.description != null) {
                Text(
                    text = stringResource(
                        if (descriptionExpanded) R.string.action_collapse else R.string.action_expand
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { descriptionExpanded = !descriptionExpanded },
                )
            }
        }

        if (related.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.detail_related),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(related, key = { it.id }) { item ->
                        BookCard(book = item, onClick = { onBookClick(item.id) })
                    }
                }
            }
        }
    }
}

private const val COLLAPSED_DESCRIPTION_LINES = 6
