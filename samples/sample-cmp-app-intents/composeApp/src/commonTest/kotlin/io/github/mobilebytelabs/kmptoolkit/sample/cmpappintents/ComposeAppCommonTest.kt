package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ComposeAppCommonTest {

    @Test
    fun platform_name_is_present() {
        val p = getPlatform()
        assertNotNull(p.name)
        assertEquals(true, p.name.isNotBlank())
    }
}
