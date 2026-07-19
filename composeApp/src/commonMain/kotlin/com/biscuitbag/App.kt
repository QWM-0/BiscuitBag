package com.biscuitbag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.ui.screens.*
import com.biscuitbag.ui.viewmodel.*

@Composable
fun App(
    repository: BiscuitBagRepository,
    onImportEpub: (() -> Unit)? = null,
    onPickCover: ((onResult: (String) -> Unit) -> Unit)? = null
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "books"
    ) {
        // 书库列表
        composable("books") {
            val viewModel = remember { BookListViewModel(repository) }
            BookListScreen(
                viewModel = viewModel,
                onAddBook = { navController.navigate("book/add") },
                onEditBook = { id -> navController.navigate("book/edit/$id") },
                onOpenBook = { id -> navController.navigate("book/$id/chapters") },
                onStats = { navController.navigate("stats") },
                onImport = { navController.navigate("import") }
            )
        }

        // 添加书籍
        composable("book/add") {
            val viewModel = remember { BookEditViewModel(repository) }
            BookEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPickCover = onPickCover
            )
        }

        // 编辑书籍
        composable(
            "book/edit/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val viewModel = remember { BookEditViewModel(repository, bookId) }
            BookEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPickCover = onPickCover
            )
        }

        // 章节列表
        composable(
            "book/{bookId}/chapters",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val viewModel = remember { ChapterListViewModel(repository, bookId) }
            ChapterListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddChapter = { navController.navigate("book/$bookId/chapter/add") },
                onEditChapter = { id -> navController.navigate("chapter/edit/$id") },
                onOpenChapter = { id -> navController.navigate("chapter/$id/read") }
            )
        }

        // 添加章节
        composable(
            "book/{bookId}/chapter/add",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val viewModel = remember { ChapterEditViewModel(repository, bookId) }
            ChapterEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 编辑章节
        composable(
            "chapter/edit/{chapterId}",
            arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            val chapter = remember { repository.getChapterById(chapterId) }
            val viewModel = remember {
                ChapterEditViewModel(repository, chapter?.bookId ?: 0, chapterId)
            }
            ChapterEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 阅读界面（饼干屑网格）
        composable(
            "chapter/{chapterId}/read",
            arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            val viewModel = remember { ReadingViewModel(repository, chapterId) }
            ReadingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 统计
        composable("stats") {
            val viewModel = remember { StatsViewModel(repository) }
            StatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 导入电子书
        composable("import") {
            onImportEpub?.invoke()
            navController.popBackStack()
        }
    }
}
