package com.momoterminal.data.model

/**
 * Data model for country list items in the UI.
 */
data class CountryListItem(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val providerName: String,
    val currency: String,
    val phonePrefix: String,
    val hasUssdSupport: Boolean
)
