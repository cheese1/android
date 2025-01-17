package mega.privacy.android.app.data.gateway

import mega.privacy.android.domain.entity.logging.LogEntry

interface LogWriterGateway {
    /**
     * Log to file
     *
     * @param logEntry
     */
    fun writeLogEntry(
        logEntry: LogEntry,
    )
}