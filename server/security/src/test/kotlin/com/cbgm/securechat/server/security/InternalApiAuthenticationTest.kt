package com.cbgm.securechat.server.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InternalApiAuthenticationTest {
    @Test
    fun configuredTokenRequiresAnExactMatch() {
        assertTrue(InternalApiAuthentication.isAuthorized("expected-token", "expected-token"))
        assertFalse(InternalApiAuthentication.isAuthorized("expected-token", "wrong-token"))
        assertFalse(InternalApiAuthentication.isAuthorized("expected-token", null))
    }

    @Test
    fun absentTokenKeepsIsolatedTestsUsable() {
        assertTrue(InternalApiAuthentication.isAuthorized(null, null))
    }
}
