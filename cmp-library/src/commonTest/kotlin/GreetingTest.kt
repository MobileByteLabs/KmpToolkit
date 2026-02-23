package io.github.template

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class GreetingTest {

    @Test
    fun testGreetingContainsPlatformName() {
        val greeting = Greeting()
        val result = greeting.greet()

        assertTrue(result.startsWith("Hello from "))
        assertTrue(result.endsWith("!"))
    }

    @Test
    fun testPersonalizedGreeting() {
        val greeting = Greeting()
        val result = greeting.greet("World")

        assertContains(result, "World")
        assertContains(result, "Hello")
    }

    @Test
    fun testPlatformNameNotEmpty() {
        val platformName = getPlatformName()

        assertTrue(platformName.isNotBlank())
    }
}
