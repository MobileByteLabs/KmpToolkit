/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalAppIntentsApi::class)
class AppIntentsDslTest {

    @Test
    fun dsl_buildsConfig_withMultipleIntents() {
        val config = appIntents {
            intent("openTransfer") {
                title = "Open Transfer"
                description = "Opens transfer screen"
                parameter("amount", ParamType.Number, isRequired = false)
                searchable(category = "Money")
                perform { params ->
                    AppIntentResult.Dialog("Transferring ${params["amount"]}")
                }
            }
            intent("checkBalance") {
                title = "Check Balance"
                description = "Reads current balance"
                searchable()
                perform { _ -> AppIntentResult.Snippet("**$100**") }
            }
        }

        assertEquals(2, config.intents.size)
        val transfer = config.intents.first { it.id == "openTransfer" }
        assertEquals("Open Transfer", transfer.title)
        assertEquals(1, transfer.parameters.size)
        assertEquals("amount", transfer.parameters[0].name)
        assertEquals(ParamType.Number, transfer.parameters[0].type)
        assertEquals(false, transfer.parameters[0].isRequired)
        assertEquals(true, transfer.searchable)
        assertEquals("Money", transfer.searchableCategory)
    }

    @Test
    fun manifest_serializes_to_canonical_json() {
        val config = appIntents {
            intent("test") {
                title = "T"
                description = "D"
                parameter("name", ParamType.Text)
                parameter("count", ParamType.Integer, isRequired = false)
            }
        }
        val json = config.serializeManifest()
        assertTrue(json.contains("\"id\":\"test\""))
        assertTrue(json.contains("\"type\":\"Text\""))
        assertTrue(json.contains("\"type\":\"Integer\""))
        assertTrue(json.contains("\"isRequired\":false"))
    }

    @Test
    fun runtime_invoke_routesToPerform_lambda() = runTest {
        AppIntents.register(
            appIntents {
                intent("greet") {
                    title = "Greet"
                    parameter("name", ParamType.Text)
                    perform { params -> AppIntentResult.Dialog("Hello, ${params["name"]}!") }
                }
            }
        )
        val result = AppIntents.invokeForTesting("greet", mapOf("name" to "World"))
        assertNotNull(result)
        assertIs<AppIntentResult.Dialog>(result)
        assertEquals("Hello, World!", (result as AppIntentResult.Dialog).message)
    }

    @Test
    fun runtime_invoke_unknownId_returnsNull() = runTest {
        AppIntents.register(appIntents { intent("known") { perform { _ -> AppIntentResult.Done } } })
        assertNull(AppIntents.invokeForTesting("unknown-id"))
    }

    @Test
    fun intent_withoutPerform_returnsFailedResult() = runTest {
        AppIntents.register(appIntents { intent("noop") { title = "X" } })
        val result = AppIntents.invokeForTesting("noop")
        assertIs<AppIntentResult.Failed>(result)
    }

    @Test
    fun paramType_entity_carriesEntityName() {
        val type = ParamType.Entity("Movie")
        assertEquals("Movie", type.entityName)
    }
}
