package com.biscuitbag

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.biscuitbag.database.DatabaseDriverFactory
import com.biscuitbag.import.EpubImporter
import com.biscuitbag.import.WeChatReadReader
import com.biscuitbag.util.ContextProvider
import com.biscuitbag.util.Logger
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val pickEpubLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importEpub(it) }
    }

    private var onCoverPicked: ((String) -> Unit)? = null
    private var pendingCoverUri: Uri? = null

    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { launchCrop(it) }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                saveCroppedBitmap(bitmap)
            } else {
                // 裁剪返回无 bitmap，用原图
                pendingCoverUri?.let { saveCoverImage(it) }
            }
        } else {
            // 裁剪取消或不支持，用原图
            pendingCoverUri?.let { saveCoverImage(it) }
        }
        pendingCoverUri = null
    }

    private fun launchCrop(uri: Uri) {
        pendingCoverUri = uri
        try {
            val intent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(uri, "image/*")
                putExtra("crop", "true")
                putExtra("aspectX", 5)
                putExtra("aspectY", 7)
                putExtra("outputX", 400)
                putExtra("outputY", 560)
                putExtra("scale", true)
                putExtra("return-data", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            cropLauncher.launch(intent)
        } catch (e: Exception) {
            // 设备不支持裁剪，直接用原图
            saveCoverImage(uri)
            pendingCoverUri = null
        }
    }

    private fun saveCroppedBitmap(bitmap: Bitmap) {
        try {
            val coverDir = File(filesDir, "covers")
            coverDir.mkdirs()
            val coverFile = File(coverDir, "cover_${System.currentTimeMillis()}.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            onCoverPicked?.invoke(coverFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            pendingCoverUri?.let { saveCoverImage(it) }
        }
    }

    private fun saveCoverImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val coverDir = File(filesDir, "covers")
            coverDir.mkdirs()
            val coverFile = File(coverDir, "cover_${System.currentTimeMillis()}.jpg")
            coverFile.outputStream().use { inputStream.copyTo(it) }
            inputStream.close()
            onCoverPicked?.invoke(coverFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 ContextProvider（必须在一切之前）
        ContextProvider.appContext = applicationContext

        // 设置全局异常捕获
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 记录异常日志
            Logger.e("CrashHandler", "未捕获异常", throwable)

            // 保存崩溃标志
            try {
                val prefs = ContextProvider.appContext
                    .getSharedPreferences("crash", MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("has_crash", true)
                    .putString("crash_log_path", Logger.getLogFilePath())
                    .apply()
            } catch (_: Exception) {
            }

            // 启动日志导出界面
            try {
                val intent = Intent(ContextProvider.appContext, LogExportActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ContextProvider.appContext.startActivity(intent)
            } catch (_: Exception) {
            }

            // 交给默认处理器（或直接结束进程）
            defaultHandler?.uncaughtException(thread, throwable)
                ?: run {
                    Process.killProcess(Process.myPid())
                    System.exit(10)
                }
        }

        val driverFactory = DatabaseDriverFactory(this)
        val database = com.biscuitbag.database.BiscuitBagDatabase(driverFactory.createDriver())
        val repository = com.biscuitbag.data.repository.BiscuitBagRepository(database)

        val weChatReadReader = WeChatReadReader()

        setContent {
            App(
                repository = repository,
                onImportEpub = {
                    pickEpubLauncher.launch(arrayOf("application/epub+zip"))
                },
                onPickCover = { onResult ->
                    onCoverPicked = onResult
                    pickCoverLauncher.launch("image/*")
                },
                fetchWeChatShelf = { cookie ->
                    weChatReadReader.fetchShelf(cookie)
                }
            )
        }
    }

    private fun importEpub(uri: Uri) {
        try {
            // 拷贝到临时文件
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val tempFile = File(cacheDir, "temp_import.epub")
            tempFile.outputStream().use { inputStream.copyTo(it) }
            inputStream.close()

            // 解析 EPUB
            val metadata = EpubImporter.parse(tempFile, cacheDir)
            tempFile.delete()

            if (metadata == null) {
                android.widget.Toast.makeText(this, "EPUB 解析失败", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            // 保存封面图
            var coverPath = ""
            if (metadata.coverBytes != null && metadata.coverBytes.isNotEmpty()) {
                val coverFile = File(filesDir, "covers/${System.currentTimeMillis()}.jpg")
                coverFile.parentFile?.mkdirs()
                coverFile.writeBytes(metadata.coverBytes)
                coverPath = coverFile.absolutePath
            }

            // 创建书籍（书本类型，书名不带《》，由 ViewModel 加）
            val driverFactory = DatabaseDriverFactory(this)
            val database = com.biscuitbag.database.BiscuitBagDatabase(driverFactory.createDriver())
            val repository = com.biscuitbag.data.repository.BiscuitBagRepository(database)

            val bookTitle = "《${metadata.title}》"
            repository.insertBook(
                title = bookTitle,
                author = metadata.author,
                totalPages = metadata.estimatedPages,
                type = 0,
                coverPath = coverPath
            )

            // 获取刚创建的书（最简单的方式：查最大的 id）
            val books = repository.getAllBooks()
            // 由于 getAllBooks 返回 Flow，这里用同步方式不大方便。
            // 改用另一种方式：直接查
            val allBooks = mutableListOf<com.biscuitbag.data.repository.BookEntity>()
            kotlinx.coroutines.runBlocking {
                repository.getAllBooks().collect { list ->
                    allBooks.addAll(list)
                }
            }
            val bookId = allBooks.maxByOrNull { it.id }?.id ?: return

            // 创建章节和饼干屑
            for ((index, chapter) in metadata.chapters.withIndex()) {
                repository.insertChapter(
                    bookId = bookId,
                    chapterNumber = index + 1,
                    title = chapter.title,
                    paragraphCount = chapter.paragraphCount
                )
            }

            android.widget.Toast.makeText(
                this,
                "导入成功：《${metadata.title}》 ${metadata.chapters.size} 章",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // 重新创建以刷新（简单粗放但有效）
            recreate()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
