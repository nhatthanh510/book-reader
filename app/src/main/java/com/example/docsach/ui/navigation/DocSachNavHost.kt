package com.example.docsach.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.docsach.ui.category.CategoryBooksScreen
import com.example.docsach.ui.category.CategoryListScreen
import com.example.docsach.ui.detail.BookDetailScreen
import com.example.docsach.ui.home.HomeScreen
import com.example.docsach.ui.reader.ReaderScreen
import com.example.docsach.ui.search.SearchScreen
import com.example.docsach.ui.shelf.ShelfScreen

@Composable
fun DocSachNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = currentEntry?.destination?.route in Routes.topLevel

    Scaffold(
        modifier = modifier,
        bottomBar = { if (showBottomBar) DocSachBottomBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onBookClick = { navController.navigate(Routes.detail(it)) },
                    onSeeMore = { navController.navigate(Routes.queryBooks(it)) },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                )
            }

            composable(Routes.SHELF) {
                ShelfScreen(onBookClick = { navController.navigate(Routes.detail(it)) })
            }

            composable(Routes.CATEGORIES) {
                CategoryListScreen(
                    onCategoryClick = { navController.navigate(Routes.queryBooks(it)) },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onBookClick = { navController.navigate(Routes.detail(it)) },
                    onBack = navController::popBackStack,
                )
            }

            composable(
                route = Routes.QUERY_BOOKS,
                arguments = listOf(navArgument(Routes.ARG_QUERY_KEY) { type = NavType.StringType }),
            ) {
                CategoryBooksScreen(
                    onBookClick = { navController.navigate(Routes.detail(it)) },
                    onBack = navController::popBackStack,
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_VOLUME_ID) { type = NavType.StringType }),
            ) {
                BookDetailScreen(
                    onBack = navController::popBackStack,
                    onRead = { navController.navigate(Routes.reader(it)) },
                    onBookClick = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                route = Routes.READER,
                arguments = listOf(navArgument(Routes.ARG_VOLUME_ID) { type = NavType.StringType }),
            ) {
                ReaderScreen(onBack = navController::popBackStack)
            }
        }
    }
}
