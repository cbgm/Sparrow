package com.cbgm.sparrow.feature.safety.domain.classifier

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import com.cbgm.sparrow.core.embedding.data.model.cosineSimilarity
import com.cbgm.sparrow.core.embedding.data.model.normalizedPrefix
import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmbeddingMessageSafetyClassifier(
    private val embedder: LocalTextEmbedder
) {
    private val mutex = Mutex()
    private var prototypeEmbeddings: Map<MessageSafetyReason, EmbeddedPrototypeSet>? = null

    suspend fun classify(text: String): Set<MessageSafetyReason> {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return emptySet()

        val messageEmbedding =
            embedder
                .embed(normalizedText, EmbeddingInputType.SEMANTIC_SIMILARITY)
                .normalizedPrefix(LocalEmbeddingModel.OUTPUT_DIMENSIONS)
        val prototypes = ensurePrototypeEmbeddings()

        val result =
            PROTOTYPES.keys.filterTo(linkedSetOf()) { reason ->
                val prototypeSet = prototypes.getValue(reason)
                val positiveSimilarity =
                    prototypeSet.positive.maxOf { prototype -> cosineSimilarity(messageEmbedding, prototype) }
                val negativeSimilarity =
                    prototypeSet.negative.maxOf { prototype -> cosineSimilarity(messageEmbedding, prototype) }

                MessageSafetyClassifierPolicy
                    .threshold(reason)
                    .matches(
                        positiveSimilarity = positiveSimilarity,
                        negativeSimilarity = negativeSimilarity
                    )
            }

        return result
    }

    suspend fun clear() {
        mutex.withLock {
            prototypeEmbeddings = null
        }
    }

    private suspend fun ensurePrototypeEmbeddings(): Map<MessageSafetyReason, EmbeddedPrototypeSet> {
        mutex.withLock { prototypeEmbeddings?.let { return it } }

        val built =
            PROTOTYPES.mapValues { (_, prototypeSet) ->
                EmbeddedPrototypeSet(
                    positive = prototypeSet.positive.map { embedPrototype(it) },
                    negative = prototypeSet.negative.map { embedPrototype(it) }
                )
            }
        return mutex.withLock {
            prototypeEmbeddings ?: built.also { prototypeEmbeddings = it }
        }
    }

    private suspend fun embedPrototype(text: String): FloatArray =
        embedder
            .embed(text, EmbeddingInputType.SEMANTIC_SIMILARITY)
            .normalizedPrefix(LocalEmbeddingModel.OUTPUT_DIMENSIONS)

    private data class PrototypeSet(
        val positive: List<String>,
        val negative: List<String>
    )

    private data class EmbeddedPrototypeSet(
        val positive: List<FloatArray>,
        val negative: List<FloatArray>
    )

    private companion object {
        val PROTOTYPES =
            mapOf(
                MessageSafetyReason.URGENT_ACTION_REQUEST to
                    PrototypeSet(
                        positive =
                            listOf(
                                "A message that pressures the recipient to act immediately because something bad will happen if they delay, such as account closure, loss, punishment, or a severe deadline.",
                                "Eine Nachricht, die den Empfänger unter Druck setzt, sofort zu handeln, weil bei Verzögerung etwas Negatives droht, etwa Kontosperre, Verlust, Strafe oder eine sehr kurze Frist."
                            ),
                        negative =
                            listOf(
                                "A normal reminder or conversation about an appointment, delivery, deadline, or date that does not threaten, frighten, or pressure the recipient into immediate action.",
                                "Eine normale Erinnerung oder Unterhaltung über Termin, Lieferung, Frist oder Datum, die den Empfänger weder bedroht noch verängstigt oder zu sofortigem Handeln drängt."
                            )
                    ),
                MessageSafetyReason.CREDENTIAL_REQUEST to
                    PrototypeSet(
                        positive =
                            listOf(
                                "A message asking the recipient to reveal or send a password, PIN, login code, one-time authentication code, verification code, or other secret account credential.",
                                "Eine Nachricht, die den Empfänger auffordert, Passwort, PIN, Anmeldecode, Einmalcode, Bestätigungscode oder andere geheime Zugangsdaten preiszugeben oder zu senden."
                            ),
                        negative =
                            listOf(
                                "A security or account conversation that discusses passwords, login codes, PINs, two-factor authentication, or credentials without asking the recipient to reveal or send the secret information.",
                                "Eine Sicherheits- oder Kontounterhaltung über Passwörter, Anmeldecodes, PINs, Zwei-Faktor-Authentifizierung oder Zugangsdaten, ohne den Empfänger zur Preisgabe oder zum Senden geheimer Informationen aufzufordern."
                            )
                    ),
                MessageSafetyReason.PAYMENT_REQUEST to
                    PrototypeSet(
                        positive =
                            listOf(
                                "A message asking the recipient to send or transfer money, buy gift cards, send cryptocurrency, pay an unexpected charge, or make an unusual financial transaction.",
                                "Eine Nachricht, die den Empfänger auffordert, Geld zu senden oder zu überweisen, Gutscheine zu kaufen, Kryptowährung zu senden, eine unerwartete Forderung zu bezahlen oder eine ungewöhnliche Finanztransaktion auszuführen."
                            ),
                        negative =
                            listOf(
                                "A normal conversation about a price, receipt, salary, invoice, completed purchase, shared expense, or payment status that does not ask for an unusual or suspicious transfer of money.",
                                "Eine normale Unterhaltung über Preis, Beleg, Gehalt, Rechnung, abgeschlossenen Kauf, geteilte Kosten oder Zahlungsstatus, ohne zu einer ungewöhnlichen oder verdächtigen Geldüberweisung aufzufordern."
                            )
                    ),
                MessageSafetyReason.PRIVATE_KEY_REQUEST to
                    PrototypeSet(
                        positive =
                            listOf(
                                "A message asking the recipient to reveal or send a private key, recovery phrase, seed phrase, wallet seed, backup words, or another cryptographic secret that controls an account or wallet.",
                                "Eine Nachricht, die den Empfänger auffordert, privaten Schlüssel, Wiederherstellungsphrase, Seed-Phrase, Wallet-Seed, Sicherungswörter oder ein anderes kryptografisches Geheimnis zur Kontrolle eines Kontos oder Wallets preiszugeben oder zu senden."
                            ),
                        negative =
                            listOf(
                                "A security or educational conversation explaining private keys, recovery phrases, seed phrases, wallet backups, or cryptographic secrets without asking the recipient to reveal or send them.",
                                "Eine Sicherheits- oder Lernunterhaltung, die private Schlüssel, Wiederherstellungsphrasen, Seed-Phrasen, Wallet-Sicherungen oder kryptografische Geheimnisse erklärt, ohne den Empfänger zur Preisgabe oder zum Senden aufzufordern."
                            )
                    )
            )
    }
}
