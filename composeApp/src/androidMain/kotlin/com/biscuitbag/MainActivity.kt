package com.biscuitbag

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.biscuitbag.database.DatabaseDriverFactory
import com.biscuitbag.import.EpubImporter
import java.io.File

class MainActivity : ComponentActivity() {

    private val pickEpubLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importEpub(it) }
    }

    private var onCoverPicked: ((String) -> Unit)? = null

    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveCoverImage(it) }
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

        val driverFactory = DatabaseDriverFactory(this)
        val database = com.biscuitbag.database.BiscuitBagDatabase(driverFactory.createDriver())
        val repository = com.biscuitbag.data.repository.BiscuitBagRepository(database)

        setContent {
            App(
                repository = repository,
                onImportEpub = {
                    pickEpubLauncher.launch(arrayOf("application/epub+zip"))
                },
                onPickCover = { onResult ->
                    onCoverPicked = onResult
                    pickCoverLauncher.launch("image/*")
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
