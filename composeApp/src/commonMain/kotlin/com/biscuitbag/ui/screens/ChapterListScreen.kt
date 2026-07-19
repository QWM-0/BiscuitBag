package com.biscuitbag.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biscuitbag.ui.viewmodel.ChapterListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    viewModel: ChapterListViewModel,
    onBack: () -> Unit,
    onAddChapter: () -> Unit,
    onEditChapter: (Long) -> Unit,
    onOpenChapter: (Long) -> Unit
) {
    val book by viewModel.book.collectAsState()
    val chapters by viewModel.chapters.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "章节") },
                navigationIcon = { TextButton(onClick = onBack) { Text("书库") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddChapter) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有章节，点击 + 添加第一个章节吧",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chapters, key = { it.chapter.id }) { item ->
                    ChapterCard(
                        item = item,
                        onClick = { onOpenChapter(item.chapter.id) },
                        onEdit = { onEditChapter(item.chapter.id) },
                        onDelete = { viewModel.deleteChapter(item.chapter.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterCard(
    item: com.biscuitbag.ui.viewmodel.ChapterWithProgress,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "第${item.chapter.chapterNumber}章 ${item.chapter.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${item.readCount}/${item.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )) { Text("删除") }
            }
        }
    }
}
