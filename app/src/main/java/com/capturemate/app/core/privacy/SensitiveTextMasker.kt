package com.capturemate.app.core.privacy

class SensitiveTextMasker {
    private val rules = listOf(
        MaskRule(Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""), "[EMAIL]"),
        MaskRule(Regex("""\b01[016789][-\s.]?\d{3,4}[-\s.]?\d{4}\b"""), "[PHONE]"),
        MaskRule(
            Regex(
                """(?:서울특별시|서울시|서울|부산광역시|부산시|부산|대구광역시|대구시|대구|인천광역시|인천시|인천|광주광역시|광주시|광주|대전광역시|대전시|대전|울산광역시|울산시|울산|세종특별자치시|세종시|세종|경기도|경기|강원특별자치도|강원도|강원|충청북도|충북|충청남도|충남|전북특별자치도|전라북도|전북|전라남도|전남|경상북도|경북|경상남도|경남|제주특별자치도|제주도|제주)(?:\s+[가-힣]{1,20}(?:시|군|구)){0,3}\s+[가-힣0-9]{1,30}(?:로|길)(?:\s*\d{1,5}(?:-\d{1,5})?)?(?:\s+[가-힣0-9\s-]{1,30}(?:동|호))?|(?:서울특별시|서울시|서울|부산광역시|부산시|부산|대구광역시|대구시|대구|인천광역시|인천시|인천|광주광역시|광주시|광주|대전광역시|대전시|대전|울산광역시|울산시|울산|세종특별자치시|세종시|세종|경기도|경기|강원특별자치도|강원도|강원|충청북도|충북|충청남도|충남|전북특별자치도|전라북도|전북|전라남도|전남|경상북도|경북|경상남도|경남|제주특별자치도|제주도|제주)(?:\s+[가-힣]{1,20}(?:시|군|구)){0,3}\s+[가-힣]{1,20}(?:읍|면|동|리)\s*\d{1,5}(?:-\d{1,5})?(?:\s+[가-힣0-9\s-]{1,30}(?:동|호))?|\b\d{1,4}동\s*\d{1,4}호\b""",
            ),
            "[ADDRESS]",
        ),
        MaskRule(Regex("""\b\d{2,6}[-\s.]?\d{2,6}[-\s.]?\d{2,8}\b"""), "[ACCOUNT_OR_ID]"),
    )

    fun mask(rawText: String): MaskedText {
        var masked = rawText
        val foundTypes = mutableSetOf<String>()

        rules.forEach { rule ->
            if (rule.pattern.containsMatchIn(masked)) {
                foundTypes += rule.placeholder.removeSurrounding("[", "]")
                masked = masked.replace(rule.pattern, rule.placeholder)
            }
        }

        return MaskedText(value = masked.trim(), detectedTypes = foundTypes.toList())
    }
}

data class MaskedText(
    val value: String,
    val detectedTypes: List<String>,
)

private data class MaskRule(
    val pattern: Regex,
    val placeholder: String,
)
