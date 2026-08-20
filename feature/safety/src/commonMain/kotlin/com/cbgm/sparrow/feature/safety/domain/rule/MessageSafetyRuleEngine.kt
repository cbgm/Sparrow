package com.cbgm.sparrow.feature.safety.domain.rule

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk

class MessageSafetyRuleEngine {
    operator fun invoke(text: String): MessageSafetyAssessment {
        if (text.isBlank()) return MessageSafetyAssessment.Safe

        val normalizedText = text.lowercase()
        val urls = URL_REGEX.findAll(text).map(MatchResult::value).toList()
        val reasons = linkedSetOf<MessageSafetyReason>()

        analyzeUrls(urls, reasons)

        val containsCredentialRequest =
            containsSensitiveRequest(
                text = normalizedText,
                sensitiveTerms = CREDENTIAL_TERMS
            ) && !containsSafetyAdvice(normalizedText)
        if (containsCredentialRequest) {
            reasons += MessageSafetyReason.CREDENTIAL_REQUEST
        }

        val containsPrivateKeyRequest =
            containsSensitiveRequest(
                text = normalizedText,
                sensitiveTerms = PRIVATE_KEY_TERMS
            ) && !containsSafetyAdvice(normalizedText)
        if (containsPrivateKeyRequest) {
            reasons += MessageSafetyReason.PRIVATE_KEY_REQUEST
        }

        val containsPaymentRequest = containsPaymentRequest(normalizedText)
        if (containsPaymentRequest) {
            reasons += MessageSafetyReason.PAYMENT_REQUEST
        }

        val containsUrgency = URGENCY_PATTERNS.any { pattern -> pattern.containsMatchIn(normalizedText) }
        if (containsUrgency) {
            reasons += MessageSafetyReason.URGENT_ACTION_REQUEST
        }

        if (
            urls.isNotEmpty() &&
            reasons.any { reason ->
                reason in LINK_REASONS ||
                    reason == MessageSafetyReason.CREDENTIAL_REQUEST ||
                    reason == MessageSafetyReason.PAYMENT_REQUEST ||
                    reason == MessageSafetyReason.URGENT_ACTION_REQUEST
            }
        ) {
            reasons += MessageSafetyReason.SUSPICIOUS_LINK
        }

        return MessageSafetyAssessment(
            risk = resolveRisk(reasons),
            reasons = reasons
        )
    }

    private fun analyzeUrls(
        urls: List<String>,
        reasons: MutableSet<MessageSafetyReason>
    ) {
        urls.forEach { url ->
            val host = extractHost(url)
            if (host.isBlank()) return@forEach

            if (isIpAddress(host)) {
                reasons += MessageSafetyReason.IP_ADDRESS_LINK
            }
            if (host in URL_SHORTENER_HOSTS) {
                reasons += MessageSafetyReason.URL_SHORTENER
            }
            if (host.contains("xn--")) {
                reasons += MessageSafetyReason.LOOKALIKE_DOMAIN
            }
            if (containsMixedScripts(host)) {
                reasons += MessageSafetyReason.MIXED_SCRIPT_DOMAIN
            }
        }
    }

    private fun resolveRisk(reasons: Set<MessageSafetyReason>): MessageSafetyRisk {
        if (reasons.isEmpty()) return MessageSafetyRisk.NONE

        if (MessageSafetyReason.PRIVATE_KEY_REQUEST in reasons) {
            return MessageSafetyRisk.HIGH
        }
        if (
            MessageSafetyReason.CREDENTIAL_REQUEST in reasons &&
            MessageSafetyReason.SUSPICIOUS_LINK in reasons
        ) {
            return MessageSafetyRisk.HIGH
        }
        if (
            MessageSafetyReason.PAYMENT_REQUEST in reasons &&
            MessageSafetyReason.URGENT_ACTION_REQUEST in reasons
        ) {
            return MessageSafetyRisk.HIGH
        }

        val score = reasons.sumOf(::riskWeight)
        return when {
            score >= HIGH_RISK_SCORE -> MessageSafetyRisk.HIGH
            score >= SUSPICIOUS_RISK_SCORE -> MessageSafetyRisk.SUSPICIOUS
            else -> MessageSafetyRisk.LOW
        }
    }

    private fun riskWeight(reason: MessageSafetyReason): Int =
        when (reason) {
            MessageSafetyReason.SUSPICIOUS_LINK -> 1
            MessageSafetyReason.LOOKALIKE_DOMAIN -> 3
            MessageSafetyReason.MIXED_SCRIPT_DOMAIN -> 3
            MessageSafetyReason.IP_ADDRESS_LINK -> 2
            MessageSafetyReason.URL_SHORTENER -> 1
            MessageSafetyReason.URGENT_ACTION_REQUEST -> 1
            MessageSafetyReason.CREDENTIAL_REQUEST -> 3
            MessageSafetyReason.PAYMENT_REQUEST -> 2
            MessageSafetyReason.PRIVATE_KEY_REQUEST -> 5
        }

    private fun containsSensitiveRequest(
        text: String,
        sensitiveTerms: Set<String>
    ): Boolean {
        if (sensitiveTerms.none(text::contains)) return false
        return REQUEST_TERMS.any(text::contains)
    }

    private fun containsPaymentRequest(text: String): Boolean =
        PAYMENT_REQUEST_PATTERNS.any { pattern -> pattern.containsMatchIn(text) } ||
            (PAYMENT_TERMS.any(text::contains) && REQUEST_TERMS.any(text::contains))

    private fun containsSafetyAdvice(text: String): Boolean =
        SAFETY_ADVICE_PATTERNS.any { pattern -> pattern.containsMatchIn(text) }

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
            if (withoutScheme.count { char -> char == ':' } == 1) {
                withoutScheme.substringBefore(':')
            } else {
                withoutScheme
            }

        return withoutPort.removePrefix("www.")
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            if (part.isEmpty() || part.length > 3 || !part.all(Char::isDigit)) {
                false
            } else {
                val value = part.toIntOrNull()
                value != null && value in 0..255
            }
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
        const val HIGH_RISK_SCORE = 5
        const val SUSPICIOUS_RISK_SCORE = 2

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

        val REQUEST_TERMS =
            setOf(
                "send ",
                "send me",
                "share ",
                "share your",
                "tell me",
                "give me",
                "enter ",
                "enter your",
                "confirm ",
                "verify ",
                "reply with",
                "sende ",
                "schick ",
                "schicke ",
                "teile ",
                "gib mir",
                "gib ",
                "eingeben",
                "gib deinen",
                "gib deine",
                "bestätige ",
                "verifiziere ",
                "überweise ",
                "zahle ",
                "bezahle "
            )

        val CREDENTIAL_TERMS =
            setOf(
                "password",
                "passwort",
                " pin",
                "pin ",
                "otp",
                "2fa",
                "verification code",
                "verification-code",
                "security code",
                "login code",
                "bestätigungscode",
                "verifizierungscode",
                "sicherheitscode",
                "anmeldecode",
                " tan",
                "tan ",
                "tan-code",
                "tancode"
            )

        val PRIVATE_KEY_TERMS =
            setOf(
                "private key",
                "private-key",
                "privater schlüssel",
                "privaten schlüssel",
                "seed phrase",
                "seed-phrase",
                "recovery phrase",
                "recovery-phrase",
                "wiederherstellungsphrase",
                "wallet seed"
            )

        val PAYMENT_TERMS =
            setOf(
                "money",
                "payment",
                "bank transfer",
                "wire transfer",
                "gift card",
                "bitcoin",
                "crypto",
                "wallet",
                "geld",
                "zahlung",
                "überweisung",
                "überweise",
                "gutschein"
            )

        val PAYMENT_REQUEST_PATTERNS =
            listOf(
                Regex("\\b(?:send|transfer|wire|pay)\\b.{0,40}\\b(?:money|payment|bitcoin|crypto|gift card)\\b"),
                Regex("(?:überweise|zahle|bezahle|sende).{0,40}(?:geld|zahlung|bitcoin|krypto|gutschein)")
            )

        val URGENCY_PATTERNS =
            listOf(
                Regex("\\burgent(?:ly)?\\b"),
                Regex("\\bimmediately\\b"),
                Regex("\\bact now\\b"),
                Regex("\\bright now\\b"),
                Regex("\\bwithin \\d+ (?:minutes?|hours?|days?)\\b"),
                Regex("\\blast warning\\b"),
                Regex("\\baccount.{0,30}(?:locked|suspended|closed)\\b"),
                Regex("\\bdringend\\b"),
                Regex("\\bsofort\\b"),
                Regex("\\bjetzt handeln\\b"),
                Regex("\\bheute noch\\b"),
                Regex("\\binnerhalb von \\d+ (?:minuten?|stunden?|tagen?)\\b"),
                Regex("\\bletzte warnung\\b"),
                Regex("\\bkonto.{0,30}(?:gesperrt|geschlossen)\\b")
            )

        val SAFETY_ADVICE_PATTERNS =
            listOf(
                Regex("\\bnever (?:send|share|give).{0,40}(?:password|code|private key|seed phrase|recovery phrase)\\b"),
                Regex("\\bdo not (?:send|share|give).{0,40}(?:password|code|private key|seed phrase|recovery phrase)\\b"),
                Regex("\\bdon't (?:send|share|give).{0,40}(?:password|code|private key|seed phrase|recovery phrase)\\b"),
                Regex("\\bniemals.{0,20}(?:passwort|code|privaten schlüssel|seed phrase|wiederherstellungsphrase).{0,20}(?:senden|teilen|weitergeben)\\b"),
                Regex("\\bnicht.{0,20}(?:passwort|code|privaten schlüssel|seed phrase|wiederherstellungsphrase).{0,20}(?:senden|teilen|weitergeben)\\b")
            )

        val LINK_REASONS =
            setOf(
                MessageSafetyReason.LOOKALIKE_DOMAIN,
                MessageSafetyReason.MIXED_SCRIPT_DOMAIN,
                MessageSafetyReason.IP_ADDRESS_LINK,
                MessageSafetyReason.URL_SHORTENER
            )
    }
}
