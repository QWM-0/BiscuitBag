package com.biscuitbag

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.import.WRShelfResponse
import com.biscuitbag.ui.screens.*
import com.biscuitbag.ui.viewmodel.*

@Composable
fun App(
    navController: NavHostController,
    repository: BiscuitBagRepository,
    onImportEpub: (() -> Unit)? = null,
    onPickCover: ((onResult: (String) -> Unit) -> Unit)? = null,
    fetchWeChatShelf: (suspend (String) -> WRShelfResponse)? = null
) {

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
                onOpenBook = { id ->
                    val book = repository.getBookById(id)
                    if (book?.thickMode == true) {
                        navController.navigate("book/$id/chapters")
                    } else {
                        navController.navigate("book/$id/read")
                    }
                },
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

        // 非厚读模式：直接阅读（自动创建或使用默认章节）
        composable(
            "book/{bookId}/read",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val chapter = remember { repository.getOrCreateDefaultChapter(bookId) }
            val viewModel = remember { ReadingViewModel(repository, chapter.id) }
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

        // 导入选择界面
        composable("import") {
            ImportOptionsScreen(
                onImportEpub = {
                    onImportEpub?.invoke()
                    navController.popBackStack()
                },
                onImportWeChat = {
                    navController.navigate("import/weread")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 微信读书导入
        composable("import/weread") {
            if (fetchWeChatShelf != null) {
                WeChatReadImportScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    fetchShelf = fetchWeChatShelf
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("微信读书导入不可用（平台未实现）")
                }
            }
        }
    }
}

/**
 * 导入方式选择界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportOptionsScreen(
    onImportEpub: () -> Unit,
    onImportWeChat: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入书籍") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "选择导入方式",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onImportEpub,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("从 EPUB 导入", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onImportWeChat,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("从微信读书导入", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
