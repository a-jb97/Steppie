package com.example.steppie.data.local

import com.example.steppie.domain.model.LocalizedText
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object LocalizedTextCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(text: LocalizedText): String = text.values.entries
        .sortedBy(Map.Entry<String, String>::key)
        .joinToString("|") { (locale, value) -> "${encodePart(locale)}:${encodePart(value)}" }

    fun decode(value: String): LocalizedText {
        val entries = value.split('|').associate { encodedEntry ->
            val parts = encodedEntry.split(':', limit = 2)
            require(parts.size == 2) { "Invalid LocalizedText database value." }
            decodePart(parts[0]) to decodePart(parts[1])
        }
        return LocalizedText(entries)
    }

    private fun encodePart(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodePart(value: String): String =
        String(decoder.decode(value), StandardCharsets.UTF_8)
}
