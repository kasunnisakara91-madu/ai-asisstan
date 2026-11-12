package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LoadedPdfDocument(
    val uri: Uri,
    val fileName: String,
    val pageCount: Int,
    val pageTexts: List<String>
)

class PdfProcessor(private val context: Context) {
    private val mutex = Mutex()
    @Volatile
    private var pdfBoxInitialized = false

    private fun ensureInitialized() {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized = true
        }
    }

    suspend fun loadPdfText(uri: Uri): LoadedPdfDocument = withContext(Dispatchers.IO) {
        ensureInitialized()
        val fileName = resolveDisplayName(uri)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val count = document.numberOfPages.coerceAtLeast(1)
                val stripper = PDFTextStripper()
                val pages = ArrayList<String>(count)
                for (page in 1..count) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val text = stripper.getText(document).trim()
                    pages.add(text)
                }
                LoadedPdfDocument(
                    uri = uri,
                    fileName = fileName,
                    pageCount = document.numberOfPages,
                    pageTexts = pages
                )
            }
        } ?: throw IllegalStateException("Unable to open the selected PDF file.")
    }

    suspend fun renderPageBitmap(uri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@use null
                        renderer.openPage(pageIndex).use { page ->
                            val scale = 2
                            val width = (page.width * scale).coerceIn(360, 1440)
                            val height = (page.height * scale).coerceIn(480, 2048)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            bitmap
                        }
                    }
                }
            }.getOrNull()
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
    }
}
