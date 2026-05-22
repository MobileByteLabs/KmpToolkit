/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 *
 * `rememberPdfGenerator()` lives in each Compose-supporting source set (android, jvm, ios,
 * macos, js, wasmJs) as a non-expect `@Composable fun`. It is not declared expect in commonMain
 * because Compose Multiplatform does not publish artifacts for tier-3 targets (tvos, watchos,
 * linux, mingw, wasmWasi), and a commonMain `@Composable expect` would force every source set
 * to depend on Compose.
 *
 * Compose-side users:  val gen = rememberPdfGenerator()
 * Non-Compose users:   val gen = PdfGenerator()  // direct constructor
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator
