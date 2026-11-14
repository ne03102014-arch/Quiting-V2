package com.quiting.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared tunnel status observed by the UI. */
object TunnelState {
    enum class Status { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private val _status = MutableStateFlow(Status.DISCONNECTED)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    var lastError: String? = null
        private set

    fun set(status: Status, error: String? = null) {
        lastError = error
        _status.value = status
    }
}
