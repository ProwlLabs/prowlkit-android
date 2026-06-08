package com.prowllabs.prowl.core.util

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

object BodyDecoder {
    fun decodeIfNeeded(data: ByteArray, contentEncoding: String?): ByteArray {
        if (data.isEmpty()) return data
        val encoding = contentEncoding?.lowercase().orEmpty()
        return when {
            encoding.contains("gzip") || isGzip(data) -> decompressGzip(data)
            encoding.contains("deflate") -> decompressDeflate(data)
            else -> data
        }
    }

    fun charsetFromContentType(contentType: String?): Charset {
        val charsetParam = contentType?.let { type ->
            Regex("""charset=([^;\s]+)""", RegexOption.IGNORE_CASE)
                .find(type)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.trim('"', '\'')
        }
        return runCatching {
            when (charsetParam?.lowercase()) {
                null, "utf-8", "utf8" -> Charsets.UTF_8
                else -> Charset.forName(charsetParam)
            }
        }.getOrDefault(Charsets.UTF_8)
    }

    fun toText(
        data: ByteArray,
        contentType: String?,
        contentEncoding: String? = null,
    ): String {
        val decoded = decodeIfNeeded(data, contentEncoding)
        return String(decoded, charsetFromContentType(contentType))
    }

    fun isImageContentType(contentType: String?): Boolean {
        val type = contentType?.lowercase().orEmpty()
        return type.startsWith("image/") && !type.contains("svg")
    }

    fun isBinaryContentType(contentType: String?): Boolean {
        val type = contentType?.lowercase().orEmpty()
        if (type.isEmpty()) return false
        if (isImageContentType(type)) return false
        if (type.contains("json") || type.contains("xml") || type.contains("html") ||
            type.contains("text") || type.contains("form")
        ) {
            return false
        }
        return type.contains("octet-stream") || type.contains("protobuf") ||
            type.contains("grpc") || type.contains("image/")
    }

    fun hexPreview(data: ByteArray, maxBytes: Int = 256): String {
        val slice = data.copyOfRange(0, minOf(data.size, maxBytes))
        val hex = slice.joinToString(" ") { "%02X".format(it) }
        return if (data.size > maxBytes) "$hex … (${data.size} bytes total)" else "$hex (${data.size} bytes)"
    }

    fun looksLikeText(data: ByteArray, contentType: String?): Boolean {
        if (isBinaryContentType(contentType)) return false
        if (contentType?.contains("json", ignoreCase = true) == true) return true
        if (contentType?.startsWith("text/", ignoreCase = true) == true) return true
        val sample = data.copyOfRange(0, minOf(data.size, 512))
        if (sample.isEmpty()) return true
        var control = 0
        for (byte in sample) {
            val b = byte.toInt() and 0xFF
            if (b == 0x09 || b == 0x0A || b == 0x0D) continue
            if (b < 0x20 || b == 0x7F) control++
        }
        return control.toDouble() / sample.size < 0.1
    }

    private fun isGzip(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()

    private const val MAX_DECOMPRESSED_BYTES = 2 * 1024 * 1024

    private fun decompressGzip(data: ByteArray): ByteArray =
        runCatching {
            GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytesUpTo(MAX_DECOMPRESSED_BYTES) }
        }.getOrElse { data }

    private fun decompressDeflate(data: ByteArray): ByteArray =
        runCatching {
            InflaterInputStream(ByteArrayInputStream(data)).use { it.readBytesUpTo(MAX_DECOMPRESSED_BYTES) }
        }.getOrElse { data }

    private fun java.io.InputStream.readBytesUpTo(maxBytes: Int): ByteArray {
        val buffer = ByteArray(minOf(maxBytes, 8192))
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (total < maxBytes) {
            val toRead = minOf(buffer.size, maxBytes - total)
            val read = read(buffer, 0, toRead)
            if (read <= 0) break
            output.write(buffer, 0, read)
            total += read
        }
        return output.toByteArray()
    }
}
