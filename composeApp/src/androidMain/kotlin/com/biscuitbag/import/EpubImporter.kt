package com.biscuitbag.import

import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class EpubMetadata(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>,
    val coverBytes: ByteArray?,
    val estimatedPages: Int
)

data class EpubChapter(
    val title: String,
    val paragraphCount: Int
)

object EpubImporter {

    fun parse(epubFile: File, cacheDir: File): EpubMetadata? {
        return try {
            ZipFile(epubFile).use { zip ->
                // 1. 找到 OPF 文件路径
                val opfPath = findOpfPath(zip) ?: return null

                // 2. 解析 OPF 获取元数据和资源路径
                val opfDir = File(opfPath).parent ?: ""
                val opfXml = zip.getInputStream(zip.getEntry(opfPath))!!.bufferedReader().readText()
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(opfXml.byteInputStream())

                // 提取标题
                val title = doc.getElementsByTagNameNS("*", "title").let { nodes ->
                    if (nodes.length > 0) nodes.item(0).textContent.trim() else "未命名书籍"
                }

                // 提取作者
                val author = doc.getElementsByTagNameNS("*", "creator").let { nodes ->
                    if (nodes.length > 0) nodes.item(0).textContent.trim() else ""
                }

                // 构建 manifest (id -> href)
                val manifest = mutableMapOf<String, String>()
                val manifestItems = doc.getElementsByTagNameNS("*", "item")
                for (i in 0 until manifestItems.length) {
                    val item = manifestItems.item(i)
                    val id = item.attributes.getNamedItem("id")?.textContent ?: continue
                    val href = item.attributes.getNamedItem("href")?.textContent ?: continue
                    manifest[id] = href
                }

                // 提取 spine（阅读顺序）
                val spineIds = mutableListOf<String>()
                val spineItems = doc.getElementsByTagNameNS("*", "itemref")
                for (i in 0 until spineItems.length) {
                    val idref = spineItems.item(i).attributes.getNamedItem("idref")?.textContent ?: continue
                    spineIds.add(idref)
                }

                // 提取封面图
                val coverBytes = extractCover(zip, doc, manifest, opfDir)

                // 提取章节（从 spine + TOC）
                val chapters = extractChapters(zip, spineIds, manifest, opfDir)

                // 估算页数
                val estimatedPages = chapters.sumOf { it.paragraphCount } / 5

                EpubMetadata(
                    title = title,
                    author = author,
                    chapters = chapters,
                    coverBytes = coverBytes,
                    estimatedPages = estimatedPages.coerceAtLeast(1)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
        val regex = Regex("""full-path=["']([^"']+)["']""")
        return regex.find(containerXml)?.groupValues?.get(1)
    }

    private fun extractCover(
        zip: ZipFile,
        opfDoc: org.w3c.dom.Document,
        manifest: Map<String, String>,
        opfDir: String
    ): ByteArray? {
        // 方法1：通过 meta name="cover" 找
        val metas = opfDoc.getElementsByTagNameNS("*", "meta")
        var coverId: String? = null
        for (i in 0 until metas.length) {
            val meta = metas.item(i)
            if (meta.attributes.getNamedItem("name")?.textContent == "cover") {
                coverId = meta.attributes.getNamedItem("content")?.textContent
                break
            }
        }

        // 方法2：找 id 含 "cover" 的图片项
        if (coverId == null) {
            val items = opfDoc.getElementsByTagNameNS("*", "item")
            for (i in 0 until items.length) {
                val item = items.item(i)
                val id = item.attributes.getNamedItem("id")?.textContent ?: continue
                val mediaType = item.attributes.getNamedItem("media-type")?.textContent ?: continue
                if (id.lowercase().contains("cover") && mediaType.startsWith("image/")) {
                    coverId = id
                    break
                }
            }
        }

        val coverHref = coverId?.let { manifest[it] } ?: return null
        val path = if (opfDir.isNotEmpty()) "$opfDir/$coverHref" else coverHref
        val entry = zip.getEntry(path) ?: zip.getEntry(coverHref) ?: return null
        return zip.getInputStream(entry).readBytes()
    }

    private fun extractChapters(
        zip: ZipFile,
        spineIds: List<String>,
        manifest: Map<String, String>,
        opfDir: String
    ): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        var chapterNum = 0

        for (idref in spineIds) {
            chapterNum++
            val href = manifest[idref] ?: continue
            val path = if (opfDir.isNotEmpty()) "$opfDir/$href" else href
            val entry = zip.getEntry(path) ?: continue

            val html = zip.getInputStream(entry).bufferedReader().readText()
            val title = extractTitle(html) ?: "第${chapterNum}章"
            val paragraphCount = countParagraphs(html)

            chapters.add(EpubChapter(title = title, paragraphCount = paragraphCount.coerceAtLeast(1)))
        }

        return chapters
    }

    private fun extractTitle(html: String): String? {
        val h1Regex = Regex("""<h1[^>]*>(.*?)</h1>""", RegexOption.IGNORE_CASE)
        val h2Regex = Regex("""<h2[^>]*>(.*?)</h2>""", RegexOption.IGNORE_CASE)
        val titleRegex = Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)

        return h1Regex.find(html)?.groupValues?.get(1)?.cleanHtml()
            ?: h2Regex.find(html)?.groupValues?.get(1)?.cleanHtml()
            ?: titleRegex.find(html)?.groupValues?.get(1)?.cleanHtml()
    }

    private fun countParagraphs(html: String): Int {
        val pRegex = Regex("""<p[\s>]""", RegexOption.IGNORE_CASE)
        return pRegex.findAll(html).count().coerceAtLeast(1)
    }

    private fun String.cleanHtml(): String {
        return this.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
