package com.fintrack.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * "Something new was stored": sent by the SMS receiver and the notification listener, so an open
 * app redraws (and the balance moves) the moment a payment arrives.
 */
object DataEvents {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changed: SharedFlow<Unit> get() = changes

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}
