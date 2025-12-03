package com.momoterminal.core.i18n.rtl

import android.content.Context
import android.view.View
import java.util.*

object RtlUtils {
    private val RTL_LANGUAGES = setOf("ar", "he", "fa", "ur")
    
    fun isRtlLanguage(languageCode: String): Boolean = languageCode in RTL_LANGUAGES
    
    fun isRtlLocale(locale: Locale): Boolean = isRtlLanguage(locale.language)
    
    fun isRtlContext(context: Context): Boolean =
        context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}
