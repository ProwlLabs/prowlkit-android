package com.prowllabs.prowl.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.prowllabs.prowl.ui.theme.ProwlColors

object ProwlJsonHighlighter {
    private val keyPattern = Regex("""\"([^\"\\]|\\.)*\"\s*:""")
    private val stringPattern = Regex("""":\s*\"([^\"\\]|\\.)*\"""")
    private val numberPattern = Regex("""\b-?\d+(\.\d+)?\b""")
    private val literalPattern = Regex("""\b(true|false|null)\b""")

    fun highlight(text: String, contentType: String?): AnnotatedString {
        if (!looksLikeJson(text, contentType)) {
            return AnnotatedString(text)
        }
        return buildAnnotatedString {
            append(text)
            applyPattern(text, keyPattern, ProwlColors.JsonKey)
            applyPattern(text, stringPattern, ProwlColors.JsonString)
            applyPattern(text, numberPattern, ProwlColors.JsonNumber)
            applyPattern(text, literalPattern, ProwlColors.JsonLiteral)
        }
    }

    fun looksLikeJson(text: String, contentType: String?): Boolean {
        val type = contentType?.lowercase().orEmpty()
        if (type.contains("json")) return true
        val trimmed = text.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun AnnotatedString.Builder.applyPattern(text: String, pattern: Regex, color: Color) {
        pattern.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
        }
    }
}
