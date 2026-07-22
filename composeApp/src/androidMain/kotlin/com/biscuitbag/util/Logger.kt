package com.biscuitbag.util

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object Logger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE_NAME = "biscuitbag.log"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    private const val KEEP_TAIL_SIZE = 2 * 1024 * 1024L // 2MB

    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private fun getOrCreateLogFile(): File? {
        if (logFile != null && logFile!!.exists()) return logFile
        if (!ContextProvider.isInitialized) return null
        try {
            val dir = File(ContextProvider.appContext.filesDir, LOG_DIR)
            dir.mkdirs()
            logFile = File(dir, LOG_FILE_NAME)
            return logFile
        } catch (e: Exception) {
            Log.e("Logger", "Failed to create log file", e)
            return null
        }
    }

    actual fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    actual fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    actual fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        Log.e(tag, msg, throwable)
        writeToFile(tag, "ERROR", msg, throwable)
    }

    actual fun getLogFilePath(): String? {
        return getOrCreateLogFile()?.absolutePath
    }

    private fun writeToFile(tag: String, level: String, msg: String, throwable: Throwable?) {
        val file = getOrCreateLogFile() ?: return
        try {
            truncateIfNeeded(file)
            val timestamp = dateFormat.format(Date())
            val sb = StringBuilder()
            sb.append("$timestamp [$level/$tag] $msg")
            if (throwable != null) {
                sb.append('\n')
                sb.append(throwable.toString())
                sb.append('\n')
                throwable.stackTrace.forEach { element ->
                    sb.append("  at $element\n")
                }
            }
            sb.append('\n')
            FileWriter(file, true).use { writer ->
                writer.append(sb.toString())
            }
        } catch (e: Exception) {
            Log.e("Logger", "Failed to write log to file", e)
        }
    }

    private fun truncateIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_FILE_SIZE) return
        try {
            val raf = RandomAccessFile(file, "rw")
            try {
                val totalLength = raf.length()
                val startPos = totalLength - KEEP_TAIL_SIZE
                // 找到 startPos 后第一个换行符，避免截断行
                raf.seek(startPos)
                var c = raf.read()
                while (c != -1 && c.toChar() != '\n') {
                    c = raf.read()
                }
                val actualStart = raf.filePointer
                val remaining = totalLength - actualStart
                val buffer = ByteArray(remaining.toInt())
                raf.seek(actualStart)
                raf.readFully(buffer)
                raf.seek(0)
                raf.write(buffer)
                raf.setLength(remaining)
            } finally {
                raf.close()
            }
        } catch (e: Exception) {
            // 截断失败，直接清空重来
            try {
                file.delete()
                file.createNewFile()
            } catch (_: Exception) {
            }
        }
    }
}
