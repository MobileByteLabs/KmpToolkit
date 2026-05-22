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

/**
 * Opt-in marker for cmp-pdf-generator v0.x APIs.
 * The public surface may change before v1.0.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "cmp-pdf-generator is experimental; API may change before v1.0. " +
        "Opt in with @OptIn(ExperimentalPdfGeneratorApi::class).",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalPdfGeneratorApi
