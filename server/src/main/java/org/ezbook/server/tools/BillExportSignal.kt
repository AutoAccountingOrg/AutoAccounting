package org.ezbook.server.tools

/**
 * Process-local post-commit signal. The server never performs external I/O;
 * the Android app registers a listener that enqueues durable background work.
 */
object BillExportSignal {
    @Volatile
    private var listener: (() -> Unit)? = null

    fun register(listener: (() -> Unit)?) {
        this.listener = listener
    }

    fun notifyCommitted() {
        listener?.invoke()
    }
}
