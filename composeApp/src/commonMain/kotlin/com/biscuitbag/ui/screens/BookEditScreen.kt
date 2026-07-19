package com.biscuitbag.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.biscuitbag.ui.viewmodel.BookEditViewModel
import com.biscuitbag.util.loadImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditScreen(
    viewModel: BookEditViewModel,
    onBack: () -> Unit,
    onPickCover: ((onResult: (String) -> Unit) -> Unit)? = null
) {
    val title by viewModel.title.collectAsState()
    val author by viewModel.author.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val bookType by viewModel.bookType.collectAsState()
    val coverPath by viewModel.coverPath.collectAsState()
    val isEdit by viewModel.isEdit.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑书籍" else "添加书籍") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面区域
            val coverBitmap = remember(coverPath) { loadImageBitmap(coverPath) }
            val coverModifier = Modifier
                .size(100.dp, 140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    // 显示已加载的封面图片，点击可重新选择
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = "封面",
                        modifier = coverModifier.then(
                            if (onPickCover != null) {
                                Modifier.clickable {
                                    onPickCover { path -> viewModel.setCoverPath(path) }
                                }
                            } else { Modifier }
                        )
                    )
                } else if (onPickCover != null) {
                    Box(
                        modifier = coverModifier.clickable {
                            onPickCover { path -> viewModel.setCoverPath(path) }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "点击设置封面",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = coverModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }

            // 类型选择
            Text("类型", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = bookType == 0, onClick = { viewModel.setBookType(0) })
                    Text("纸质书", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = bookType == 1, onClick = { viewModel.setBookType(1) })
                    Text("电子书", modifier = Modifier.padding(start = 4.dp))
                }
            }

            // 厚读模式开关
            val thickMode by viewModel.thickMode.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("厚读模式", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (thickMode) "分章节管理饼干屑" else "整本书共用饼干屑",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = thickMode, onCheckedChange = { viewModel.setThickMode(it) })
            }

            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(if (bookType == 0) "书名（自动加《》）" else "名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("留空自动填充「未命名书籍」") }
            )

            OutlinedTextField(
                value = author,
                onValueChange = { viewModel.updateAuthor(it) },
                label = { Text("作者") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = totalPages,
                onValueChange = { viewModel.updateTotalPages(it) },
                label = { Text(if (bookType == 0) "总页数" else "总段落数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(if (bookType == 0) "纸质书：页数 × 4 自动估算饼干屑" else "电子书：按百分比划分饼干屑")
                }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "保存修改" else "添加书籍")
            }
        }
    }
}
