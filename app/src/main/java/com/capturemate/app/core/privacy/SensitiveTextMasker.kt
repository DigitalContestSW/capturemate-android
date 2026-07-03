package com.capturemate.app.core.privacy

class SensitiveTextMasker {
    private val rules = listOf(
        MaskRule(Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""), "[EMAIL]"),
        MaskRule(Regex("""\b01[016789][-\s.]?\d{3,4}[-\s.]?\d{4}\b"""), "[PHONE]"),
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
