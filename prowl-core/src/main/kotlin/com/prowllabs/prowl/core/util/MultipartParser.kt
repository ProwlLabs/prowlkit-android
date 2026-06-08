package com.prowllabs.prowl.core.util

import com.prowllabs.prowl.core.model.MultipartPart

object MultipartParser {
    fun parse(body: ByteArray, contentType: String?): List<MultipartPart> {
        val type = contentType?.lowercase().orEmpty()
        if (!type.contains("multipart/")) return emptyList()
        val boundary = extractBoundary(contentType) ?: return emptyList()
        val text = body.toString(Charsets.ISO_8859_1)
        val delimiter = "--$boundary"
        return text.split(delimiter)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "--" }
            .mapNotNull { section -> parsePart(section) }
    }

    private fun extractBoundary(contentType: String?): String? {
        if (contentType == null) return null
        val match = Regex("""boundary=(["']?)([^"';\\s]+)\1""", RegexOption.IGNORE_CASE)
            .find(contentType)
        return match?.groupValues?.get(2)
    }

    private fun parsePart(section: String): MultipartPart? {
        val headerBodySplit = section.indexOf("\r\n\r\n").let { idx ->
            if (idx >= 0) idx else section.indexOf("\n\n")
        }
        if (headerBodySplit < 0) return null
        val separatorLen = if (section.contains("\r\n\r\n")) 4 else 2
        val headerBlock = section.substring(0, headerBodySplit)
        val bodyRaw = section.substring(headerBodySplit + separatorLen)
            .trimEnd('\r', '\n', '-')

        val headers = mutableMapOf<String, String>()
        headerBlock.lines().filter { it.contains(':') }.forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
        }

        val disposition = headers["Content-Disposition"].orEmpty()
        val name = Regex("""name="([^"]*)"""").find(disposition)?.groupValues?.get(1)
        val fileName = Regex("""filename="([^"]*)"""").find(disposition)?.groupValues?.get(1)
            ?: Regex("""filename\*=UTF-8''([^;\\s]+)""").find(disposition)?.groupValues?.get(1)
        val partContentType = headers["Content-Type"]
        val bodyBytes = bodyRaw.toByteArray(Charsets.ISO_8859_1)
        val isBinary = BodyDecoder.isBinaryContentType(partContentType) ||
            bodyBytes.any { it < 0x09 || (it in 0x0E..0x1F) }

        val preview = when {
            isBinary -> BodyDecoder.hexPreview(bodyBytes, 64)
            else -> bodyRaw.take(512).let { if (bodyRaw.length > 512) "$it…" else it }
        }

        return MultipartPart(
            name = name,
            fileName = fileName,
            contentType = partContentType,
            headers = headers,
            sizeBytes = bodyBytes.size,
            textPreview = preview,
            isBinary = isBinary,
        )
    }
}
