package com.momoterminal.core.common.di

import javax.inject.Qualifier

/**
 * Qualifier for application-scoped CoroutineScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
