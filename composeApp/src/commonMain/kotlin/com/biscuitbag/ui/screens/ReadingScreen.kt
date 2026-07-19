package com.biscuitbag.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.biscuitbag.data.repository.BreadcrumbEntity
import com.biscuitbag.ui.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onBack: () -> Unit
) {
    val chapter by viewModel.chapter.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val readCount by viewModel.readCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chapter?.title ?: "阅读")
                        Text(
                            "${readCount}/${totalCount} 已读 (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("章节") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (breadcrumbs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有饼干屑数据")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(breadcrumbs, key = { it.id }) { crumb ->
                        BreadcrumbCircle(
                            index = crumb.paragraphIndex,
                            isRead = crumb.isRead,
                            onClick = { viewModel.toggleBreadcrumb(crumb) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreadcrumbCircle(
    index: Int,
    isRead: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isRead)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isRead)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isRead) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRead) "✓" else "${index + 1}",
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isRead) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
