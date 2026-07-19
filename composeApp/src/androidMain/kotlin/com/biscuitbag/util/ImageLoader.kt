package com.biscuitbag.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun loadImageBitmap(path: String): ImageBitmap? {
    if (path.isBlank()) return null
    return try {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
