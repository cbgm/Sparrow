package com.cbgm.sparrow.core.ui.device.background

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneMakerTest {
    @Test
    fun manufacturersAndTheirSubBrandsAreRecognized() {
        assertEquals(PhoneMaker.XIAOMI, PhoneMaker.of("Xiaomi"))
        assertEquals(PhoneMaker.XIAOMI, PhoneMaker.of("POCO"))
        assertEquals(PhoneMaker.HONOR, PhoneMaker.of("HONOR"))
        assertEquals(PhoneMaker.OPPO, PhoneMaker.of("OnePlus"))
        assertEquals(PhoneMaker.OPPO, PhoneMaker.of("realme"))
        assertEquals(PhoneMaker.VIVO, PhoneMaker.of(" iQOO "))
        assertEquals(PhoneMaker.SAMSUNG, PhoneMaker.of("samsung"))
    }

    @Test
    fun phonesThatLeaveAppsRunningGetNoHelp() {
        assertNull(PhoneMaker.of("Google"))
        assertNull(PhoneMaker.of("motorola"))
        assertNull(PhoneMaker.of(""))
    }

    @Test
    fun everyMakerNamesAtLeastOneSettingsScreen() {
        assertTrue(PhoneMaker.entries.all { maker -> maker.settingsScreens.isNotEmpty() })
    }
}
