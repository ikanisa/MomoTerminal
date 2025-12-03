package com.momoterminal.core.i18n.backend

import com.momoterminal.core.i18n.locale.AppLocale
import com.momoterminal.core.i18n.locale.LocalePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleInterceptor @Inject constructor(
    private val localePreferences: LocalePreferences
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val locale = runBlocking { localePreferences.currentLocale.first() }
        
        val request = chain.request().newBuilder()
            .addHeader("Accept-Language", locale.toAcceptLanguageHeader())
            .build()
        
        return chain.proceed(request)
    }
    
    private fun AppLocale.toAcceptLanguageHeader(): String =
        if (regionCode != null) "$languageCode-$regionCode,$languageCode;q=0.9,en;q=0.8"
        else "$languageCode,en;q=0.8"
}
