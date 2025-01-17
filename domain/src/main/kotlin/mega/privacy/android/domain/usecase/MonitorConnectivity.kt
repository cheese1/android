package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow


/**
 * Monitor connectivity
 *
 */
fun interface MonitorConnectivity {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
