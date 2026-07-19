package com.biscuitbag.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 从文件路径加载 ImageBitmap。
 * 返回 null 表示路径为空或加载失败。
 */
expect fun loadImageBitmap(path: String): ImageBitmap?
