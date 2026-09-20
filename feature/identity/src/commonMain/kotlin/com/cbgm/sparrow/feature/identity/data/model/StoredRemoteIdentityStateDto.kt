package com.cbgm.sparrow.feature.identity.data.model

/** Storage values shared with existing Room rows; never rename without a migration. */
internal enum class StoredKeyExchangeStatusDto { ONE_WAY, MUTUAL }

/** Storage values shared with existing Room rows; never rename without a migration. */
internal enum class StoredVerificationStatusDto { UNVERIFIED, VERIFIED }
