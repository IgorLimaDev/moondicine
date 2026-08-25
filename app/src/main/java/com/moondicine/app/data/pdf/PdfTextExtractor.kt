package com.moondicine.app.data.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfTextExtractor @Inject constructor(
    private val context: Context
) {
    init {
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extracts all text content from a PDF file at the given URI.
     */
    suspend fun extractText(pdfUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo PDF"))

            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()

            // Configure stripper for better text extraction
            stripper.sortByPosition = true
            stripper.addMoreFormatting = true

            val text = stripper.getText(document)
            document.close()
            inputStream.close()

            if (text.isBlank()) {
                Result.failure(Exception("O PDF parece estar vazio ou contém apenas imagens"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
                Result.failure(Exception("Não foi possível extrair o texto do PDF: ${e.message}"))
        }
    }

    /**
     * Extracts text and returns basic metadata about the PDF.
     */
    suspend fun extractWithMetadata(pdfUri: Uri): Result<PdfContent> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo PDF"))

            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true

            val text = stripper.getText(document)
            val pageCount = document.numberOfPages
            document.close()
            inputStream.close()

            Result.success(
                PdfContent(
                    text = text,
                    pageCount = pageCount,
                    charCount = text.length,
                    wordCount = text.split("\\s+".toRegex()).size
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível extrair o PDF: ${e.message}"))
        }
    }
}

data class PdfContent(
    val text: String,
    val pageCount: Int,
    val charCount: Int,
    val wordCount: Int
)
