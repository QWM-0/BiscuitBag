package com.biscuitbag.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biscuitbag.ui.viewmodel.ChapterEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditScreen(
    viewModel: ChapterEditViewModel,
    onBack: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val paragraphCount by viewModel.paragraphCount.collectAsState()
    val usePageEstimate by viewModel.usePageEstimate.collectAsState()
    val pagesForChapter by viewModel.pagesForChapter.collectAsState()
    val isEdit by viewModel.isEdit.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑章节" else "添加章节") },
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
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("章节标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("留空自动填充「第N章」") }
            )

            Text(
                "饼干屑（段落）数量设置",
                style = MaterialTheme.typography.titleSmall
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = usePageEstimate,
                    onClick = { viewModel.setUsePageEstimate(true) }
                )
                Text("按页数估算", modifier = Modifier.padding(start = 4.dp))
            }

            if (usePageEstimate) {
                OutlinedTextField(
                    value = pagesForChapter,
                    onValueChange = { viewModel.updatePagesForChapter(it) },
                    label = { Text("本章页数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("按每页约 4 段估算，预估 ${viewModel.estimatedParagraphs()} 个饼干屑")
                    }
                )
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = !usePageEstimate,
                    onClick = { viewModel.setUsePageEstimate(false) }
                )
                Text("手动输入段落数", modifier = Modifier.padding(start = 4.dp))
            }

            if (!usePageEstimate) {
                OutlinedTextField(
                    value = paragraphCount,
                    onValueChange = { viewModel.updateParagraphCount(it) },
                    label = { Text("段落数量 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "保存修改" else "添加章节")
            }
        }
    }
}
