package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default [StatisticsRepository] implementation
 */
class DefaultStatisticsRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val statisticsPreferencesGateway: StatisticsPreferencesGateway,
) : StatisticsRepository {

    override suspend fun sendEvent(eventID: Int, message: String) = withContext(ioDispatcher) {
        megaApiGateway.sendEvent(eventID, message)
    }

    override suspend fun getMediaDiscoveryClickCount(): Int =
        statisticsPreferencesGateway.getClickCount().first()

    override suspend fun setMediaDiscoveryClickCount(clickCount: Int) =
        statisticsPreferencesGateway.setClickCount(clickCount)


    override suspend fun getMediaDiscoveryClickCountFolder(mediaHandle: Long): Int =
        statisticsPreferencesGateway.getClickCountFolder(mediaHandle).first()


    override suspend fun setMediaDiscoveryClickCountFolder(
        clickCountFolder: Int,
        mediaHandle: Long,
    ) = statisticsPreferencesGateway.setClickCountFolder(clickCountFolder, mediaHandle)

}