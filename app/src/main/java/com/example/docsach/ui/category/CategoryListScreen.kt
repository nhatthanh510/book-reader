package com.example.docsach.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.docsach.R
import com.example.docsach.data.model.bookCategories
import com.example.docsach.ui.components.DocSachTopAppBar

/** The "Danh mục" tab: every genre the store browses by. */
@Composable
fun CategoryListScreen(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { DocSachTopAppBar(title = stringResource(R.string.nav_categories)) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(bookCategories, key = { it.key }) { category ->
                ListItem(
                    headlineContent = { Text(stringResource(category.labelRes)) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onCategoryClick(category.key) },
                )
                HorizontalDivider()
            }
        }
    }
}
