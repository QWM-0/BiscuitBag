package com.biscuitbag

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import com.biscuitbag.util.ContextProvider
import com.biscuitbag.util.Logger
import java.io.File

class LogExportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertDialog.Builder(this)
            .setTitle("应用异常")
            .setMessage("应用出现异常，建议导出日志帮助排查问题。")
            .setPositiveButton("导出日志") { _, _ ->
                exportLog()
            }
            .setNegativeButton("忽略") { _, _ ->
                clearFlagAndRestart()
            }
            .setCancelable(false)
            .show()
    }

    private fun exportLog() {
        val logPath = Logger.getLogFilePath()
        if (logPath == null) {
            Toast.makeText(this, "日志文件不存在", Toast.LENGTH_SHORT).show()
            clearFlagAndRestart()
            return
        }

        val logFile = File(logPath)
        if (!logFile.exists()) {
            Toast.makeText(this, "日志文件不存在", Toast.LENGTH_SHORT).show()
            clearFlagAndRestart()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "com.biscuitbag.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "导出日志"))
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        clearFlagAndRestart()
    }

    private fun clearFlagAndRestart() {
        // 清除崩溃标志
        ContextProvider.appContext
            .getSharedPreferences("crash", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // 重新进入主页
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
