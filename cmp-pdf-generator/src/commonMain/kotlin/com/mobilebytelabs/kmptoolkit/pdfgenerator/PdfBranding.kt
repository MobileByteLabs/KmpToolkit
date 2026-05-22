/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.datetime.LocalDate

/**
 * Logo source for the PDF header. Consumers inject; the library renders.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfLogo {
    public data class Svg(
        public val bytes: ByteArray,
    ) : PdfLogo() {
        override fun equals(other: Any?): Boolean = other is Svg && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    public data class Png(
        public val bytes: ByteArray,
    ) : PdfLogo() {
        override fun equals(other: Any?): Boolean = other is Png && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    public data class DataUri(
        public val uri: String,
    ) : PdfLogo()

    public object None : PdfLogo()
}

/**
 * Theme colors + typography for PDF rendering.
 *
 * @param accentColorHex Hex color (incl. leading #) for borders / accents.
 * @param headerColorHex Hex color for H1/H2 headings.
 * @param tableRowEvenHex Hex color for alternating table rows.
 * @param borderColorHex Hex color for table borders.
 * @param fontFamily CSS font-family stack. e.g. `"Roboto, Helvetica, Arial, sans-serif"`.
 * @param fontScale Multiplier on base 8pt size. 1.0 = default. 1.25 = ~10pt base.
 * @param fontEmbedding Raw TTF bytes to embed as the primary font. Null = use system font.
 */
@ExperimentalPdfGeneratorApi
public data class PdfTheme(
    public val accentColorHex: String = "#33618D",
    public val headerColorHex: String = "#1976d2",
    public val tableRowEvenHex: String = "#fafafa",
    public val borderColorHex: String = "#e0e0e0",
    public val fontFamily: String = "Roboto, Helvetica, Arial, sans-serif",
    public val fontScale: Float = 1.0f,
    public val fontEmbedding: ByteArray? = null,
) {
    init {
        require(fontScale > 0f) { "fontScale must be > 0" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfTheme) return false
        return accentColorHex == other.accentColorHex &&
            headerColorHex == other.headerColorHex &&
            tableRowEvenHex == other.tableRowEvenHex &&
            borderColorHex == other.borderColorHex &&
            fontFamily == other.fontFamily &&
            fontScale == other.fontScale &&
            (fontEmbedding?.contentEquals(other.fontEmbedding) ?: (other.fontEmbedding == null))
    }

    override fun hashCode(): Int {
        var r = accentColorHex.hashCode()
        r = 31 * r + headerColorHex.hashCode()
        r = 31 * r + tableRowEvenHex.hashCode()
        r = 31 * r + borderColorHex.hashCode()
        r = 31 * r + fontFamily.hashCode()
        r = 31 * r + fontScale.hashCode()
        r = 31 * r + (fontEmbedding?.contentHashCode() ?: 0)
        return r
    }

    public companion object {
        public fun defaults(): PdfTheme = PdfTheme()
    }
}

/**
 * Watermark overlay — text and/or image, with opacity + rotation.
 *
 * @param opacity 0.0 (invisible) to 1.0 (opaque).
 * @param rotationDeg Rotation in degrees. Negative rotates counterclockwise.
 */
@ExperimentalPdfGeneratorApi
public data class Watermark(
    public val text: String? = null,
    public val image: ImageSource? = null,
    public val opacity: Float = 0.1f,
    public val rotationDeg: Int = -45,
) {
    init {
        require(opacity in 0f..1f) { "opacity must be in [0.0, 1.0]" }
        require(text != null || image != null) { "Watermark must have text or image" }
    }
}

/**
 * Bundle of all branding settings — logo, footer, theme, date formatter, watermark.
 * Inject into templates, or pass per-call to [PdfGenerator.generateFromHtml] /
 * [PdfGenerator.generate].
 *
 * @param logo Logo for the header. Use [PdfLogo.None] to omit.
 * @param poweredByText Optional footer text. `null` omits the footer entirely.
 * @param theme Color + typography settings.
 * @param dateFormatter Function converting a [LocalDate] to a display string.
 * @param watermark Optional watermark applied to every page.
 */
@ExperimentalPdfGeneratorApi
public data class PdfBranding(
    public val logo: PdfLogo = PdfLogo.None,
    public val poweredByText: String? = null,
    public val theme: PdfTheme = PdfTheme.defaults(),
    public val dateFormatter: (LocalDate) -> String = ::defaultDateFormat,
    public val watermark: Watermark? = null,
) {
    public companion object {
        /** No logo, no footer, default theme — for "pure content" PDFs. */
        public fun none(): PdfBranding = PdfBranding()

        /** Default theme with no logo but generic "Powered by KmpToolkit" footer. */
        public fun default(): PdfBranding =
            PdfBranding(
                poweredByText = "Powered by KmpToolkit",
            )

        /**
         * Back-compat: reproduces mifos-x reference branding (colors only — consumers must supply their own logo).
         * Intended for code migrating off mifos-x's private utility.
         */
        public fun mifosDefault(logo: PdfLogo = PdfLogo.None): PdfBranding =
            PdfBranding(
                logo = logo,
                poweredByText = "Powered by Mifos",
                theme =
                    PdfTheme(
                        accentColorHex = "#33618D",
                        headerColorHex = "#1976d2",
                    ),
            )
    }
}

/** Default date format: `dd/MM/yyyy`. */
@ExperimentalPdfGeneratorApi
public fun defaultDateFormat(date: LocalDate): String {
    val d = date.day.toString().padStart(2, '0')
    val m = date.monthNumber.toString().padStart(2, '0')
    return "$d/$m/${date.year}"
}
