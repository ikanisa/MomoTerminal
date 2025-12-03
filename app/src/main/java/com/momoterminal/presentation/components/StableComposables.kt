package com.momoterminal.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Stable wrappers for Compose performance optimization.
 * 
 * Compose skips recomposition when parameters are stable.
 * These wrappers ensure collections and callbacks are stable.
 */

/**
 * Stable wrapper for lambda callbacks.
 * Prevents unnecessary recomposition when callbacks are recreated.
 */
@Stable
class StableCallback<T>(val callback: (T) -> Unit) {
    operator fun invoke(value: T) = callback(value)
}

@Stable
class StableAction(val action: () -> Unit) {
    operator fun invoke() = action()
}

/**
 * Stable holder for list data.
 */
@Immutable
data class StableList<T>(val items: ImmutableList<T>) {
    companion object {
        fun <T> empty(): StableList<T> = StableList(persistentListOf())
        fun <T> of(items: List<T>): StableList<T> = StableList(items.toImmutableList())
    }
}

/**
 * Extension to convert List to StableList.
 */
fun <T> List<T>.toStable(): StableList<T> = StableList.of(this)

/**
 * Stable holder for nullable values.
 */
@Stable
data class StableValue<T>(val value: T?)

/**
 * Remember a stable callback.
 */
@Stable
fun <T> stableCallback(callback: (T) -> Unit) = StableCallback(callback)

@Stable
fun stableAction(action: () -> Unit) = StableAction(action)
