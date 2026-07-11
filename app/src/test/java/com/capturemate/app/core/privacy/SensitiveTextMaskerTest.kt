package com.capturemate.app.core.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveTextMaskerTest {
    private val masker = SensitiveTextMasker()

    @Test
    fun masksCommonSensitiveTypesIncludingAddress() {
        val masked = masker.mask(
            """
            연락처는 test@example.com, 010-1234-5678 입니다.
            주소는 서울특별시 강남구 테헤란로 123 101동 1203호 입니다.
            계좌 후보는 123-456-789012 입니다.
            """.trimIndent(),
        )

        assertTrue(masked.detectedTypes.contains("EMAIL"))
        assertTrue(masked.detectedTypes.contains("PHONE"))
        assertTrue(masked.detectedTypes.contains("ADDRESS"))
        assertTrue(masked.detectedTypes.contains("ACCOUNT_OR_ID"))
        assertTrue(masked.value.contains("[EMAIL]"))
        assertTrue(masked.value.contains("[PHONE]"))
        assertTrue(masked.value.contains("[ADDRESS]"))
        assertTrue(masked.value.contains("[ACCOUNT_OR_ID]"))
    }

    @Test
    fun keepsTextWithoutSensitiveTypes() {
        val rawText = "오늘 할 일은 OCR 성능 테스트입니다."

        val masked = masker.mask(rawText)

        assertEquals(rawText, masked.value)
        assertTrue(masked.detectedTypes.isEmpty())
    }
}
