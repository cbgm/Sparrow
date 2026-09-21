package com.cbgm.sparrow.feature.settings.device

import android.content.res.Resources

class AndroidSystemLanguageProvider : SystemLanguageProvider {
    override fun getLanguageTag(): String =
        Resources.getSystem().configuration.locales[0]?.toLanguageTag() ?: DEFAULT_LANGUAGE

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
    }
}
