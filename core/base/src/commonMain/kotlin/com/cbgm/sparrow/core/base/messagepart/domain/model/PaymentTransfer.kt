package com.cbgm.sparrow.core.base.messagepart.domain.model

/** Authenticated manual settlement event. Bank details are deliberately never included. */
data class PaymentTransfer(
    override val id: String,
    val transferId: String,
    val boardId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val reference: String,
    val status: PaymentTransferStatus,
    val occurredAtEpochMilliseconds: Long
) : MessagePart

enum class PaymentTransferStatus { SENT, RECEIVED }

object PaymentTransferPolicy {
    fun requireValid(event: PaymentTransfer) {
        require(
            listOf(event.id, event.transferId, event.boardId, event.fromMemberId, event.toMemberId)
                .all { it.isNotBlank() && it.length <= 256 }
        ) { "Invalid payment identifiers" }
        require(event.id == "${event.transferId}-${event.status.name.lowercase()}") { "Payment event ID does not match its status" }
        require(event.fromMemberId != event.toMemberId) { "Payment participants must differ" }
        require(event.amountMinor in 1L..99_999_999_999L) { "Invalid payment amount" }
        require(event.currencyCode == "EUR") { "SEPA payments require EUR" }
        require(event.reference.matches(Regex("SPARROW-[A-Za-z0-9-]{1,60}"))) { "Invalid settlement reference" }
        require(event.occurredAtEpochMilliseconds > 0L) { "Invalid payment time" }
    }

    fun requireActor(event: PaymentTransfer, actorMemberId: String) {
        requireValid(event)
        val expected = if (event.status == PaymentTransferStatus.SENT) event.fromMemberId else event.toMemberId
        require(actorMemberId == expected) { "Only the payer can mark sent; only the recipient can confirm receipt" }
    }

    fun matches(sent: PaymentTransfer, receipt: PaymentTransfer): Boolean =
        sent.transferId == receipt.transferId && sent.boardId == receipt.boardId &&
            sent.fromMemberId == receipt.fromMemberId && sent.toMemberId == receipt.toMemberId &&
            sent.amountMinor == receipt.amountMinor && sent.currencyCode == receipt.currencyCode &&
            sent.reference == receipt.reference
}
