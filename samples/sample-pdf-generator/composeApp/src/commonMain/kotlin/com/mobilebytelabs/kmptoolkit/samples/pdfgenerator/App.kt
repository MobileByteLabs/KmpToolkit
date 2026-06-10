/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.samples.pdfgenerator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.pdfgenerator.EdgeMargins
import com.mobilebytelabs.kmptoolkit.pdfgenerator.MarkdownPdfAdapter
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PageConfig
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PageSize
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfBranding
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGenerator
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfOutput
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfProgressEvent
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfResult
import com.mobilebytelabs.kmptoolkit.pdfgenerator.pdf
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptData
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptLineItem
import com.mobilebytelabs.kmptoolkit.pdfgenerator.templates.ReceiptTemplate
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplePdfGeneratorApp(generator: PdfGenerator) {
    var lastResult by remember { mutableStateOf("Choose a demo mode below.") }
    var progressText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(generator) {
        generator.progressFlow().collect { ev ->
            progressText =
                when (ev) {
                    PdfProgressEvent.Started -> "Started…"
                    is PdfProgressEvent.PageRendered -> "Page ${ev.pageNum}/${ev.total ?: "?"}"
                    PdfProgressEvent.Finalizing -> "Finalizing…"
                    is PdfProgressEvent.Complete -> "Complete (${ev.byteCount} bytes)"
                    is PdfProgressEvent.Failed -> "Failed: ${ev.error.message}"
                }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("cmp-pdf-generator demos") })
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Status: $lastResult", style = MaterialTheme.typography.bodySmall)
                Text("Progress: $progressText", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch { lastResult = runInvoiceDemo(generator) }
                    },
                ) { Text("1 · Invoice (HTML route → Share)") }

                Button(
                    onClick = {
                        scope.launch { lastResult = runReceiptDemo(generator) }
                    },
                ) { Text("2 · Receipt (Template → Save)") }

                Button(
                    onClick = {
                        scope.launch { lastResult = runMarkdownDemo(generator) }
                    },
                ) { Text("3 · Markdown → PDF (Save)") }

                Button(
                    onClick = {
                        scope.launch { lastResult = runDslDemo(generator) }
                    },
                ) { Text("4 · DSL `pdf { … }` (ByteArray)") }

                Button(
                    onClick = {
                        scope.launch { lastResult = runMultiPageDslDemo(generator) }
                    },
                ) { Text("5 · Multi-page DSL (ByteArray)") }
            }
        }
    }
}

private suspend fun runInvoiceDemo(gen: PdfGenerator): String {
    val html = InvoiceFixture.template(PdfBranding.default()).generateHtml()
    val result =
        gen.generateFromHtml(
            html = html,
            output = PdfOutput.Share,
            pageConfig = PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(15)),
            fileName = "custom_file_name",
        )
    return when (result) {
        is PdfResult.Success -> "Invoice generated · ${result.byteCount} bytes"
        is PdfResult.Failure -> "Invoice failed: ${result.error.message}"
    }
}

private suspend fun runReceiptDemo(gen: PdfGenerator): String {
    val receipt = ReceiptData(
        merchantName = "Sample Co",
        merchantAddress = "123 Sample Street, Sampleville",
        receiptNumber = "RCP-2026-0042",
        date = LocalDate(2026, 5, 22),
        items = listOf(
            ReceiptLineItem("Coffee", "$4.50"),
            ReceiptLineItem("Bagel", "$3.50"),
            ReceiptLineItem("Donation", "$1.00"),
        ),
        subtotal = "$9.00",
        tax = "$0.90",
        total = "$9.90",
        paymentMethod = "Card ****1234",
        footer = "Thank you!",
    )
    val html = ReceiptTemplate(PdfBranding.default(), receipt).generateHtml()
    val result = gen.generateFromHtml(
        html = html,
        output = PdfOutput.Save,
        pageConfig = PageConfig(size = PageSize.A4),
        fileName = "custom_file_name",
    )
    return when (result) {
        is PdfResult.Success -> "Receipt saved · ${result.byteCount} bytes"
        is PdfResult.Failure -> "Receipt failed: ${result.error.message}"
    }
}

private suspend fun runMarkdownDemo(gen: PdfGenerator): String {
    val md =
        """
        # Markdown → PDF Demo

        This PDF was generated from Markdown source.

        ## Features

        - Headings (h1-h6)
        - **Bold** and _italic_
        - Lists (ordered + unordered)
        - GFM tables:

        | Feature | Status |
        |---------|--------|
        | Tables  | ✅     |
        | Code    | ✅     |
        | Images  | URL only |

        ```kotlin
        val doc = pdf { page { text("hi") } }
        ```

        > Blockquotes work too.
        """.trimIndent()
    val html = MarkdownPdfAdapter.markdownToHtml(md, PdfBranding.default())
    val result = gen.generateFromHtml(
        html = html,
        output = PdfOutput.Save,
        fileName = "custom_file_name",
    )
    return when (result) {
        is PdfResult.Success -> "Markdown PDF saved · ${result.byteCount} bytes"
        is PdfResult.Failure -> "Markdown failed: ${result.error.message}"
    }
}

private suspend fun runDslDemo(gen: PdfGenerator): String {
    val doc =
        pdf {
            pageConfig(PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(20)))
            branding(PdfBranding.default())
            page {
                heading(1, "DSL Demo Report")
                text("This PDF was constructed programmatically using the pdf { … } DSL.")
                spacer(8)
                heading(2, "Key Metrics")
                table {
                    header {
                        cell("Metric")
                        cell("Value")
                        cell("Trend")
                    }
                    row {
                        cell("Active users")
                        cell("12,345")
                        cell("↑ 8%")
                    }
                    row {
                        cell("Revenue")
                        cell("$50,000")
                        cell("↑ 12%")
                    }
                    row {
                        cell("Churn")
                        cell("3.2%")
                        cell("↓ 0.4%")
                    }
                }
                spacer(8)
                divider()
                text("Generated via cmp-pdf-generator v0.1.0")
            }
        }
    val result = gen.generate(
        document = doc,
        output = PdfOutput.ByteArrayOutput,
        fileName = "custom_file_name",
    )
    return when (result) {
        is PdfResult.Success -> {
            val magicOk = result.bytes?.startsWith(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            "DSL bytes: ${result.byteCount} (magic ok: $magicOk)"
        }

        is PdfResult.Failure -> {
            "DSL failed: ${result.error.message}"
        }
    }
}

private suspend fun runMultiPageDslDemo(gen: PdfGenerator): String {
    val doc =
        pdf {
            pageConfig(PageConfig(size = PageSize.A4, margins = EdgeMargins.uniform(20)))
            branding(PdfBranding.default())
            repeat(5) { idx ->
                page {
                    heading(1, "Page ${idx + 1} of 5")
                    text("Auto-pagination demo. Each page rendered to a separate PDF page.")
                    spacer(4)
                    repeat(15) { row ->
                        text("Line ${row + 1}: lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                    }
                }
            }
        }
    val result = gen.generate(
        document = doc,
        output = PdfOutput.ByteArrayOutput,
        fileName = "custom_file_name",
    )
    return when (result) {
        is PdfResult.Success -> "Multi-page bytes: ${result.byteCount}"
        is PdfResult.Failure -> "Multi-page failed: ${result.error.message}"
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (i in prefix.indices) if (this[i] != prefix[i]) return false
    return true
}
