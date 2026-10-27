package com.repairdesk.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.repairdesk.app.R
import com.repairdesk.app.data.Format
import com.repairdesk.app.data.JobWithPayments
import java.io.File
import java.io.FileOutputStream

/**
 * Job receipt as a PDF, built with the platform PdfDocument API — no
 * third-party dependency. The same file is used for both actions:
 * printing through PrintManager, and sharing through FileProvider.
 * The receipt is a record of the job, not a tax invoice: it lists the
 * estimate, what has been paid, and what is still due.
 */
object ReceiptPdf {

    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 22f

    /** Renders the receipt into [output]; shared by print and share. */
    private fun writePdf(data: JobWithPayments, output: FileOutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
        }
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        var y = MARGIN + 22f
        fun line(text: String, p: android.graphics.Paint = paint) {
            canvas.drawText(text, MARGIN, y, p)
            y += LINE_HEIGHT
        }
        val job = data.job
        line("RepairDesk — Job Receipt", titlePaint)
        y += 10f
        line("Job: ${job.jobCode}  |  Status: ${job.statusEnum.label}")
        line("Date: ${Format.date(System.currentTimeMillis())}")
        y += 10f
        line("Customer: ${job.customerName} (${job.phone})")
        line("Device: ${job.device}")
        line("Issue: ${job.issue}")
        if (job.accessories.isNotBlank()) line("Accessories: ${job.accessories}")
        line("Promised: ${Format.date(job.promisedDate)}")
        y += 10f
        line("Estimate: ${Format.money(job.estimate)}")
        line("Paid so far: ${Format.money(data.paidSoFar)}")
        line("Due: ${Format.money(data.due)}")
        y += 10f
        data.payments.forEach { payment ->
            line("- ${Format.money(payment.amount)} via ${payment.method} on ${Format.date(payment.paidAt)}")
        }
        document.finishPage(page)
        document.writeTo(output)
        document.close()
    }

    private fun receiptFile(context: Context, jobCode: String): File {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        return File(dir, "RepairDesk-$jobCode.pdf")
    }

    /** Shares the receipt PDF with any app that accepts PDFs (WhatsApp, Gmail…). */
    fun share(context: Context, data: JobWithPayments) {
        try {
            val file = receiptFile(context, data.job.jobCode)
            FileOutputStream(file).use { writePdf(data, it) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.share_receipt))
            )
        } catch (e: Exception) {
            Toast.makeText(context, R.string.receipt_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Prints the receipt through the system print dialog. */
    fun print(context: Context, data: JobWithPayments) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "RepairDesk-${data.job.jobCode}"
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(
                            android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
                        )
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileOutputStream(destination?.fileDescriptor).use {
                            writePdf(data, it)
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            },
            null
        )
    }
}
