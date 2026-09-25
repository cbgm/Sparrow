package com.cbgm.sparrow.feature.settings.device

/** Reads the phone language without consulting Sparrow's overridden UI locale. */
interface SystemLanguageProvider {
    fun getLanguageTag(): String
}
