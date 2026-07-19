package com.biscuitbag.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.import.WRBook
import com.biscuitbag.import.WRShelfResponse
import kotlinx.coroutines.launch

/**
 * 屏幕状态
 */
private sealed class ScreenState {
    /** 输入 Cookie */
    data object InputCookie : ScreenState()
    /** 正在请求书架 */
    data object Loading : ScreenState()
    /** 书架列表展示 */
    data class BookList(val response: WRShelfResponse) : ScreenState()
    /** 正在导入 */
    data class Importing(val bookName: String) : ScreenState()
    /** 导入完成 */
    data class Success(val count: Int) : ScreenState()
    /** 错误提示 */
    data class Error(val message: String) : ScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatReadImportScreen(
    repository: BiscuitBagRepository,
    onBack: () -> Unit,
    fetchShelf: suspend (cookie: String) -> WRShelfResponse
) {
    val scope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.InputCookie) }
    var cookie by remember { mutableStateOf("") }
    // <bookId, isChecked>
    val checkedBooks = remember { mutableMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("从微信读书导入") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        when (val state = screenState) {
            is ScreenState.InputCookie -> {
                CookieInputContent(
                    modifier = Modifier.padding(padding),
                    cookie = cookie,
                    onCookieChange = { cookie = it },
                    onFetch = {
                        if (cookie.isBlank()) return@CookieInputContent
                        screenState = ScreenState.Loading
                        scope.launch {
                            val result = fetchShelf(cookie)
                            screenState = if (result.success && result.books.isNotEmpty()) {
                                // 初始化勾选状态
                                result.books.forEach { checkedBooks[it.bookId] = true }
                                ScreenState.BookList(result)
                            } else if (!result.success) {
                                ScreenState.Error("获取失败，请检查 Cookie 是否有效")
                            } else {
                                ScreenState.Error("书架上没有书籍")
                            }
                        }
                    }
                )
            }

            is ScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("正在获取书架...")
                    }
                }
            }

            is ScreenState.BookList -> {
                BookListContent(
                    modifier = Modifier.padding(padding),
                    books = state.response.books,
                    checkedBooks = checkedBooks,
                    onToggle = { bookId -> checkedBooks[bookId] = !(checkedBooks[bookId] ?: true) },
                    onImport = {
                        val selected = state.response.books.filter { checkedBooks[it.bookId] == true }
                        if (selected.isEmpty()) return@BookListContent

                        scope.launch {
                            var importedCount = 0
                            for (book in selected) {
                                screenState = ScreenState.Importing(book.title)
                                importWeChatBook(repository, book)
                                importedCount++
                            }
                            screenState = ScreenState.Success(importedCount)
                        }
                    }
                )
            }

            is ScreenState.Importing -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("正在导入: ${state.bookName}")
                    }
                }
            }

            is ScreenState.Success -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "导入成功！共导入 ${state.count} 本书",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onBack) {
                            Text("返回书库")
                        }
                    }
                }
            }

            is ScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { screenState = ScreenState.InputCookie }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookieInputContent(
    modifier: Modifier = Modifier,
    cookie: String,
    onCookieChange: (String) -> Unit,
    onFetch: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "使用说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "1. 在电脑浏览器中打开 weread.qq.com 并登录\n" +
            "2. 按 F12 打开开发者工具\n" +
            "3. 切换到 Application → Cookies → weread.qq.com\n" +
            "4. 复制 wr_skey 的值（完整的 Cookie 字符串）\n" +
            "5. 粘贴到下方输入框中",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = cookie,
            onValueChange = onCookieChange,
            label = { Text("Cookie") },
            placeholder = { Text("wr_skey=xxx; wr_pf=xxx") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onFetch,
            modifier = Modifier.fillMaxWidth(),
            enabled = cookie.isNotBlank()
        ) {
            Text("获取书架")
        }
    }
}

@Composable
private fun BookListContent(
    modifier: Modifier = Modifier,
    books: List<WRBook>,
    checkedBooks: MutableMap<String, Boolean>,
    onToggle: (String) -> Unit,
    onImport: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 统计和操作栏
        val selectedCount = books.count { checkedBooks[it.bookId] == true }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "共 ${books.size} 本，已选 $selectedCount 本",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onImport,
                enabled = selectedCount > 0
            ) {
                Text("导入选中书籍")
            }
        }

        // 书籍列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(books, key = { it.bookId }) { book ->
                BookItem(
                    book = book,
                    isChecked = checkedBooks[book.bookId] == true,
                    onToggle = { onToggle(book.bookId) }
                )
            }
        }
    }
}

@Composable
private fun BookItem(
    book: WRBook,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() }
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title.ifBlank { "未命名书籍" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.author.isNotBlank()) {
                    Text(
                        book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (book.pageCount > 0) {
                        Text(
                            "${book.pageCount} 页",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (book.chapterCount > 0) {
                        Text(
                            "${book.chapterCount} 章",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (book.readingProgress > 0) {
                        Text(
                            "进度 ${(book.readingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 将微信读书的一本书导入到本地数据库。
 */
private suspend fun importWeChatBook(
    repository: BiscuitBagRepository,
    book: WRBook
) {
    // 确定页数/段落数
    val totalPages = if (book.pageCount > 0) book.pageCount else 100

    // 插入书籍（type=1 电子书），获取 bookId
    // 书名不带《》，由 BookEditScreen 行为决定
    val bookId = repository.insertBook(
        title = book.title.ifBlank { "未命名" },
        author = book.author,
        totalPages = totalPages,
        type = 1,        // 电子书
        coverPath = "",
        thickMode = true
    )

    // 创建章节和饼干屑
    val chapterCount = if (book.chapterCount > 0) book.chapterCount else 1
    val paragraphsPerChapter = (totalPages / chapterCount).coerceAtLeast(1)

    for (i in 1..chapterCount) {
        repository.insertChapter(
            bookId = bookId,
            chapterNumber = i,
            title = "第${i}章",
            paragraphCount = paragraphsPerChapter
        )
    }
}
