package com.cbgm.sparrow.feature.safety.data.analyzer

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason

class MessageSafetyStructuralAnalyzer {
    operator fun invoke(text: String): Set<MessageSafetyReason> {
        if (text.isBlank()) return emptySet()

        val reasons = linkedSetOf<MessageSafetyReason>()
        URL_REGEX.findAll(text).map(MatchResult::value).forEach { url ->
            val host = extractHost(url)
            if (host.isBlank()) return@forEach

            if (isIpAddress(host)) reasons += MessageSafetyReason.IP_ADDRESS_LINK
            if (host in URL_SHORTENER_HOSTS) reasons += MessageSafetyReason.URL_SHORTENER
            if (host.contains("xn--")) reasons += MessageSafetyReason.LOOKALIKE_DOMAIN
            if (containsMixedScripts(host)) reasons += MessageSafetyReason.MIXED_SCRIPT_DOMAIN
        }

        if (reasons.any { it in STRUCTURAL_LINK_REASONS }) {
            reasons += MessageSafetyReason.SUSPICIOUS_LINK
        }
        return reasons
    }

    private fun extractHost(url: String): String {
        val withoutScheme =
            url
                .substringAfter("://", url)
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .substringAfterLast('@')
                .trim()
                .trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
                .lowercase()

        val withoutPort =
            if (withoutScheme.count { it == ':' } == 1) withoutScheme.substringBefore(':') else withoutScheme
        return withoutPort.removePrefix("www.")
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all(Char::isDigit) &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private fun containsMixedScripts(host: String): Boolean {
        var containsLatin = false
        var containsCyrillicOrGreek = false
        host.forEach { char ->
            when (char.code) {
                in LATIN_BASIC_RANGE,
                in LATIN_EXTENDED_RANGE -> containsLatin = true
                in GREEK_RANGE,
                in CYRILLIC_RANGE -> containsCyrillicOrGreek = true
            }
        }
        return containsLatin && containsCyrillicOrGreek
    }

    private companion object {
        val LATIN_BASIC_RANGE = 0x0041..0x007A
        val LATIN_EXTENDED_RANGE = 0x00C0..0x024F
        val GREEK_RANGE = 0x0370..0x03FF
        val CYRILLIC_RANGE = 0x0400..0x052F

        val URL_REGEX = Regex("(?i)\\b(?:https?://|www\\.)[^\\s<>()]+")
        val URL_SHORTENER_HOSTS =
            setOf(
                "bit.ly",
                "tinyurl.com",
                "t.co",
                "goo.gl",
                "ow.ly",
                "buff.ly",
                "is.gd",
                "tiny.one",
                "shorturl.at"
            )
        val STRUCTURAL_LINK_REASONS =
            setOf(
                MessageSafetyReason.LOOKALIKE_DOMAIN,
                MessageSafetyReason.MIXED_SCRIPT_DOMAIN,
                MessageSafetyReason.IP_ADDRESS_LINK,
                MessageSafetyReason.URL_SHORTENER
            )
    }
}
