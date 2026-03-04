package com.mobilebytelabs.kmptoolkit.appupdate

import com.mobilebytelabs.kmptoolkit.appupdate.test.AppUpdateTestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateResultTest {

    // ========================================================================
    // Success Tests
    // ========================================================================

    @Test
    fun successIsSuccessTrue() {
        val result = UpdateResult.Success(AppUpdateTestFixtures.UPDATE_AVAILABLE)
        assertTrue(result.isSuccess)
    }

    @Test
    fun successIsUpdateAvailableWhenAvailable() {
        val result = UpdateResult.Success(AppUpdateTestFixtures.UPDATE_AVAILABLE)
        assertTrue(result.isUpdateAvailable)
    }

    @Test
    fun successIsUpdateAvailableWhenNotAvailable() {
        val result = UpdateResult.Success(AppUpdateTestFixtures.UPDATE_NOT_AVAILABLE)
        assertFalse(result.isUpdateAvailable)
    }

    @Test
    fun successUpdateInfoOrNullReturnsInfo() {
        val result = UpdateResult.Success(AppUpdateTestFixtures.UPDATE_AVAILABLE)
        val info = result.updateInfoOrNull()
        assertNotNull(info)
        assertEquals(AppUpdateTestFixtures.UPDATE_AVAILABLE, info)
    }

    @Test
    fun successErrorMessageNull() {
        val result = UpdateResult.Success(AppUpdateTestFixtures.UPDATE_AVAILABLE)
        assertNull(result.errorMessage)
    }

    // ========================================================================
    // Error Tests
    // ========================================================================

    @Test
    fun errorIsSuccessFalse() {
        val result = UpdateResult.Error("Network error")
        assertFalse(result.isSuccess)
    }

    @Test
    fun errorIsUpdateAvailableFalse() {
        val result = UpdateResult.Error("Network error")
        assertFalse(result.isUpdateAvailable)
    }

    @Test
    fun errorUpdateInfoOrNullReturnsNull() {
        val result = UpdateResult.Error("Network error")
        assertNull(result.updateInfoOrNull())
    }

    @Test
    fun errorErrorMessageReturnsMessage() {
        val result = UpdateResult.Error("Network error")
        assertEquals("Network error", result.errorMessage)
    }

    @Test
    fun errorCauseAccessible() {
        val cause = RuntimeException("Root cause")
        val result = UpdateResult.Error("Network error", cause)
        assertEquals(cause, result.cause)
    }

    @Test
    fun errorCauseNull() {
        val result = UpdateResult.Error("Network error")
        assertNull(result.cause)
    }

    // ========================================================================
    // NotSupported Tests
    // ========================================================================

    @Test
    fun notSupportedIsSuccessFalse() {
        val result = UpdateResult.NotSupported("Platform not supported")
        assertFalse(result.isSuccess)
    }

    @Test
    fun notSupportedIsUpdateAvailableFalse() {
        val result = UpdateResult.NotSupported("Platform not supported")
        assertFalse(result.isUpdateAvailable)
    }

    @Test
    fun notSupportedUpdateInfoOrNullReturnsNull() {
        val result = UpdateResult.NotSupported("Platform not supported")
        assertNull(result.updateInfoOrNull())
    }

    @Test
    fun notSupportedErrorMessageReturnsReason() {
        val result = UpdateResult.NotSupported("Platform not supported")
        assertEquals("Platform not supported", result.errorMessage)
    }

    // ========================================================================
    // Cancelled Tests
    // ========================================================================

    @Test
    fun cancelledIsSuccessFalse() {
        val result = UpdateResult.Cancelled
        assertFalse(result.isSuccess)
    }

    @Test
    fun cancelledIsUpdateAvailableFalse() {
        val result = UpdateResult.Cancelled
        assertFalse(result.isUpdateAvailable)
    }

    @Test
    fun cancelledUpdateInfoOrNullReturnsNull() {
        val result = UpdateResult.Cancelled
        assertNull(result.updateInfoOrNull())
    }

    @Test
    fun cancelledErrorMessageNull() {
        val result = UpdateResult.Cancelled
        assertNull(result.errorMessage)
    }
}
