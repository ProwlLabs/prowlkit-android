package com.prowllabs.prowl.core.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pretty-prints JSON while preserving real Unicode characters (Arabic, CJK, emoji, etc.).
 * Android's [JSONObject.toString] escapes non-ASCII as \\uXXXX which is hard to read.
 */
object JsonPrettyPrinter {
    fun format(text: String): String = runCatching {
        val trimmed = text.trim()
        when {
            trimmed.startsWith("{") -> formatObject(JSONObject(trimmed), indent = 0)
            trimmed.startsWith("[") -> formatArray(JSONArray(trimmed), indent = 0)
            else -> text
        }
    }.getOrElse { text }

    fun format(obj: JSONObject): String = formatObject(obj, indent = 0)

    fun format(array: JSONArray): String = formatArray(array, indent = 0)

    private fun formatObject(obj: JSONObject, indent: Int): String {
        val keys = obj.keys().asSequence().toList()
        if (keys.isEmpty()) return "{}"
        val pad = indent(indent)
        val inner = indent(indent + 1)
        return buildString {
            append("{\n")
            keys.forEachIndexed { index, key ->
                append(inner)
                append(quote(key))
                append(": ")
                append(formatValue(obj.get(key), indent + 1))
                if (index < keys.lastIndex) append(',')
                append('\n')
            }
            append(pad)
            append('}')
        }
    }

    private fun formatArray(array: JSONArray, indent: Int): String {
        if (array.length() == 0) return "[]"
        val pad = indent(indent)
        val inner = indent(indent + 1)
        return buildString {
            append("[\n")
            for (index in 0 until array.length()) {
                append(inner)
                append(formatValue(array.get(index), indent + 1))
                if (index < array.length() - 1) append(',')
                append('\n')
            }
            append(pad)
            append(']')
        }
    }

    private fun formatValue(value: Any?, indent: Int): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> formatObject(value, indent)
        is JSONArray -> formatArray(value, indent)
        is Number, is Boolean -> value.toString()
        else -> quote(value.toString())
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u%04x".format(char.code))
                } else {
                    append(char)
                }
            }
        }
        append('"')
    }

    private fun indent(level: Int): String = "  ".repeat(level)
}
