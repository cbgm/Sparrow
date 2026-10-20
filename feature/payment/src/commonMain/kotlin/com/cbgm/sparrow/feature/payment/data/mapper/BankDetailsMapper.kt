package com.cbgm.sparrow.feature.payment.data.mapper

import com.cbgm.sparrow.feature.payment.data.model.BankDetailsDto
import com.cbgm.sparrow.feature.payment.domain.model.BankDetails

internal fun BankDetails.toDto() = BankDetailsDto(accountHolder, iban)

internal fun BankDetailsDto.toBankDetails() = BankDetails(accountHolder, iban)
