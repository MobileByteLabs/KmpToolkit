package io.github.template

import kotlin.test.Test
import kotlin.test.assertContains

class AndroidGreetingTest {

    @Test
    fun testAndroidPlatformName() {
        val platformName = getPlatformName()
        assertContains(platformName, "Android")
    }
}
