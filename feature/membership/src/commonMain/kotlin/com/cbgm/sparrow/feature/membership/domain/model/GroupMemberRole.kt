package com.cbgm.sparrow.feature.membership.domain.model

/** Membership roles exposed without leaking data-layer constants to other features. */
fun String.isGroupAdminRole(): Boolean = this == "OWNER" || this == "ADMIN"
