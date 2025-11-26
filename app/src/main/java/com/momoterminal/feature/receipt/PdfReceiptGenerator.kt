package com.momoterminal.feature.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generator for PDF receipts using Android's PdfDocument API.
 */
@Singleton
class PdfReceiptGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // A4 page dimensions at 72 DPI
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        
        // Margins
        private const val MARGIN_LEFT = 50f
        private const val MARGIN_RIGHT = 50f
        private const val MARGIN_TOP = 50f
        
        // Colors
        private const val COLOR_SUCCESS = 0xFF4CAF50.toInt()
        private const val COLOR_PENDING = 0xFFFF9800.toInt()
        private const val COLOR_FAILED = 0xFFF44336.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFF212121.toInt()
        private const val COLOR_TEXT_SECONDARY = 0xFF757575.toInt()
        private const val COLOR_DIVIDER = 0xFFE0E0E0.toInt()
        
        // Font sizes
        private const val FONT_SIZE_TITLE = 24f
        private const val FONT_SIZE_SUBTITLE = 18f
        private const val FONT_SIZE_BODY = 12f
        private const val FONT_SIZE_SMALL = 10f
        private const val FONT_SIZE_AMOUNT = 32f
        
        private const val LINE_HEIGHT = 20f
        private const val SECTION_SPACING = 24f
    }
    
    /**
     * Generate a PDF receipt and return the file path.
     */
    suspend fun generateReceipt(receiptData: ReceiptData): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        var yPosition = MARGIN_TOP
        val contentWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
        
        // Draw header
        yPosition = drawHeader(canvas, yPosition, contentWidth)
        
        // Draw status badge
        yPosition = drawStatusBadge(canvas, yPosition, receiptData)
        
        // Draw amount section
        yPosition = drawAmountSection(canvas, yPosition, receiptData)
        
        // Draw transaction details
        yPosition = drawTransactionDetails(canvas, yPosition, receiptData, contentWidth)
        
        // Draw sender/recipient info
        yPosition = drawPartyInfo(canvas, yPosition, receiptData, contentWidth)
        
        // Draw amount breakdown
        if (receiptData.fee > 0) {
            yPosition = drawAmountBreakdown(canvas, yPosition, receiptData, contentWidth)
        }
        
        // Draw merchant info if available
        receiptData.merchantInfo?.let { merchant ->
            yPosition = drawMerchantInfo(canvas, yPosition, merchant, contentWidth)
        }
        
        // Draw footer
        drawFooter(canvas, receiptData)
        
        document.finishPage(page)
        
        // Save to file
        val receiptsDir = File(context.cacheDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        
        val fileName = "receipt_${receiptData.transactionId}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(receiptsDir, fileName)
        
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
        
        document.close()
        
        Timber.d("PDF receipt generated: ${outputFile.absolutePath}")
        outputFile
    }
    
    private fun drawHeader(canvas: Canvas, startY: Float, contentWidth: Float): Float {
        var y = startY
        
        val titlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = FONT_SIZE_TITLE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val subtitlePaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = FONT_SIZE_BODY
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = PAGE_WIDTH / 2f
        
        canvas.drawText("MomoTerminal", centerX, y, titlePaint)
        y += LINE_HEIGHT * 1.5f
        
        canvas.drawText("Transaction Receipt", centerX, y, subtitlePaint)
        y += SECTION_SPACING
        
        // Draw divider
        y = drawDivider(canvas, y, contentWidth)
        
        return y
    }
    
    private fun drawStatusBadge(canvas: Canvas, startY: Float, receiptData: ReceiptData): Float {
        var y = startY + SECTION_SPACING / 2
        
        val statusColor = when (receiptData.status) {
            TransactionStatus.SUCCESS -> COLOR_SUCCESS
            TransactionStatus.PENDING -> COLOR_PENDING
            TransactionStatus.FAILED, TransactionStatus.CANCELLED -> COLOR_FAILED
        }
        
        val paint = Paint().apply {
            color = statusColor
            textSize = FONT_SIZE_SUBTITLE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = PAGE_WIDTH / 2f
        canvas.drawText(receiptData.statusDisplayName.uppercase(), centerX, y, paint)
        
        return y + SECTION_SPACING
    }
    
    private fun drawAmountSection(canvas: Canvas, startY: Float, receiptData: ReceiptData): Float {
        var y = startY
        
        val amountPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = FONT_SIZE_AMOUNT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val labelPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = FONT_SIZE_BODY
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = PAGE_WIDTH / 2f
        
        canvas.drawText(receiptData.formattedAmount, centerX, y, amountPaint)
        y += LINE_HEIGHT * 1.5f
        
        canvas.drawText(receiptData.typeDisplayName, centerX, y, labelPaint)
        y += SECTION_SPACING
        
        return y
    }
    
    private fun drawTransactionDetails(
        canvas: Canvas,
        startY: Float,
        receiptData: ReceiptData,
        contentWidth: Float
    ): Float {
        var y = drawSectionHeader(canvas, startY, "Transaction Details")
        
        y = drawKeyValue(canvas, y, "Transaction ID", receiptData.transactionId)
        y = drawKeyValue(canvas, y, "Reference", receiptData.referenceNumber)
        y = drawKeyValue(canvas, y, "Date", receiptData.formattedDate)
        y = drawKeyValue(canvas, y, "Time", receiptData.formattedTime)
        
        receiptData.description?.let {
            y = drawKeyValue(canvas, y, "Description", it)
        }
        
        y = drawDivider(canvas, y + LINE_HEIGHT / 2, contentWidth)
        
        return y
    }
    
    private fun drawPartyInfo(
        canvas: Canvas,
        startY: Float,
        receiptData: ReceiptData,
        contentWidth: Float
    ): Float {
        var y = drawSectionHeader(canvas, startY, "Sender")
        
        y = drawKeyValue(canvas, y, "Name", receiptData.senderName)
        y = drawKeyValue(canvas, y, "Phone", receiptData.senderPhone)
        
        receiptData.recipientName?.let { name ->
            y += LINE_HEIGHT / 2
            y = drawSectionHeader(canvas, y, "Recipient")
            y = drawKeyValue(canvas, y, "Name", name)
            receiptData.recipientPhone?.let { phone ->
                y = drawKeyValue(canvas, y, "Phone", phone)
            }
        }
        
        y = drawDivider(canvas, y + LINE_HEIGHT / 2, contentWidth)
        
        return y
    }
    
    private fun drawAmountBreakdown(
        canvas: Canvas,
        startY: Float,
        receiptData: ReceiptData,
        contentWidth: Float
    ): Float {
        var y = drawSectionHeader(canvas, startY, "Amount Breakdown")
        
        y = drawKeyValue(canvas, y, "Amount", receiptData.formattedAmount)
        y = drawKeyValue(canvas, y, "Fee", receiptData.formattedFee)
        y = drawKeyValue(canvas, y, "Total", receiptData.formattedTotal, bold = true)
        
        y = drawDivider(canvas, y + LINE_HEIGHT / 2, contentWidth)
        
        return y
    }
    
    private fun drawMerchantInfo(
        canvas: Canvas,
        startY: Float,
        merchant: MerchantInfo,
        contentWidth: Float
    ): Float {
        var y = drawSectionHeader(canvas, startY, "Merchant Information")
        
        y = drawKeyValue(canvas, y, "Name", merchant.name)
        y = drawKeyValue(canvas, y, "Code", merchant.code)
        
        merchant.address?.let {
            y = drawKeyValue(canvas, y, "Address", it)
        }
        
        merchant.phone?.let {
            y = drawKeyValue(canvas, y, "Phone", it)
        }
        
        return y + LINE_HEIGHT
    }
    
    private fun drawSectionHeader(canvas: Canvas, startY: Float, title: String): Float {
        val paint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = FONT_SIZE_BODY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText(title, MARGIN_LEFT, startY, paint)
        
        return startY + LINE_HEIGHT * 1.5f
    }
    
    private fun drawKeyValue(
        canvas: Canvas,
        startY: Float,
        key: String,
        value: String,
        bold: Boolean = false
    ): Float {
        val keyPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = FONT_SIZE_BODY
        }
        
        val valuePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = FONT_SIZE_BODY
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textAlign = Paint.Align.RIGHT
        }
        
        canvas.drawText(key, MARGIN_LEFT, startY, keyPaint)
        canvas.drawText(value, PAGE_WIDTH - MARGIN_RIGHT, startY, valuePaint)
        
        return startY + LINE_HEIGHT
    }
    
    private fun drawDivider(canvas: Canvas, y: Float, contentWidth: Float): Float {
        val paint = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, paint)
        
        return y + LINE_HEIGHT
    }
    
    private fun drawFooter(canvas: Canvas, receiptData: ReceiptData) {
        val footerY = PAGE_HEIGHT - MARGIN_TOP
        
        val paint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = FONT_SIZE_SMALL
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = PAGE_WIDTH / 2f
        
        canvas.drawText("Thank you for using MomoTerminal", centerX, footerY - LINE_HEIGHT * 2, paint)
        canvas.drawText("Generated on ${receiptData.formattedDateTime}", centerX, footerY - LINE_HEIGHT, paint)
        canvas.drawText("For support, contact support@momoterminal.com", centerX, footerY, paint)
    }
    
    /**
     * Share a receipt PDF via Android share sheet.
     */
    fun shareReceipt(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Transaction Receipt")
            putExtra(Intent.EXTRA_TEXT, "Please find attached your transaction receipt from MomoTerminal.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Get the receipts cache directory.
     */
    fun getReceiptsDirectory(): File {
        val dir = File(context.cacheDir, "receipts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Clean up old receipts (older than 7 days).
     */
    suspend fun cleanupOldReceipts() = withContext(Dispatchers.IO) {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        
        getReceiptsDirectory().listFiles()?.forEach { file ->
            if (file.lastModified() < sevenDaysAgo) {
                file.delete()
                Timber.d("Deleted old receipt: ${file.name}")
            }
        }
    }
}
