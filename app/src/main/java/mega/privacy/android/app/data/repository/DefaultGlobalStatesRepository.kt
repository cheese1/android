package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import javax.inject.Inject

class DefaultGlobalStatesRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : GlobalStatesRepository {

    @Deprecated("See documentation for individual replacements to use instead.")
    override fun monitorGlobalUpdates(): Flow<GlobalUpdate> = megaApiGateway.globalUpdates
}
