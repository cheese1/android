package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.failWithException
import mega.privacy.android.app.data.extensions.isType
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.gateway.MonitorMultiFactorAuth
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NoLoggedInUserException
import mega.privacy.android.domain.exception.NotMasterBusinessAccountException
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [AccountRepository]
 *
 * @property myAccountInfoFacade
 * @property megaApiGateway
 * @property megaChatApiGateway
 * @property monitorMultiFactorAuth
 * @property ioDispatcher
 * @property userUpdateMapper
 * @property dbH
 */
@ExperimentalContracts
class DefaultAccountRepository @Inject constructor(
    private val myAccountInfoFacade: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val monitorMultiFactorAuth: MonitorMultiFactorAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userUpdateMapper: UserUpdateMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
) : AccountRepository {
    override suspend fun getUserAccount(): UserAccount = withContext(ioDispatcher) {
        val user = megaApiGateway.getLoggedInUser()
        UserAccount(
            userId = user?.let { UserId(it.handle) },
            email = user?.email ?: "",
            isBusinessAccount = megaApiGateway.isBusinessAccount,
            isMasterBusinessAccount = megaApiGateway.isMasterBusinessAccount,
            accountTypeIdentifier = myAccountInfoFacade.accountTypeId,
            accountTypeString = myAccountInfoFacade.accountTypeString,
        )
    }

    override fun isAccountDataStale(): Boolean =
        databaseEntryIsStale() || storageCapacityUsedIsBlank()

    private fun databaseEntryIsStale() = DBUtil.callToAccountDetails().also {
        Timber.d("Check the last call to getAccountDetails")
    }

    private fun storageCapacityUsedIsBlank() =
        myAccountInfoFacade.storageCapacityUsedAsFormattedString.isBlank()

    override fun requestAccount() = myAccountInfoFacade.requestAccountDetails()

    override suspend fun setUserHasLoggedIn() {
        localStorageGateway.setUserHasLoggedIn()
    }

    override fun isMultiFactorAuthAvailable() = megaApiGateway.multiFactorAuthAvailable()

    @Throws(MegaException::class)
    override suspend fun isMultiFactorAuthEnabled(): Boolean = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.multiFactorAuthEnabled(
                megaApiGateway.accountEmail,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onMultiFactorAuthCheckRequestFinish(continuation)
                )
            )
        }
    }

    private fun onMultiFactorAuthCheckRequestFinish(
        continuation: Continuation<Boolean>,
    ) = { request: MegaRequest, error: MegaError ->
        if (request.isType(MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK)) {
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else continuation.failWithError(error)
        }
    }

    override fun monitorMultiFactorAuthChanges() =
        monitorMultiFactorAuth.getEvents()

    override suspend fun requestDeleteAccountLink() = withContext<Unit>(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.cancelAccount(
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = onDeleteAccountRequestFinished(continuation)
                )
            )
        }
    }

    private fun onDeleteAccountRequestFinished(continuation: Continuation<Unit>) =
        { request: MegaRequest, error: MegaError ->
            if (request.isType(MegaRequest.TYPE_GET_CANCEL_LINK)) {
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        continuation.resumeWith(Result.success(Unit))
                    }
                    MegaError.API_EACCESS -> continuation.failWithException(
                        NoLoggedInUserException(
                            error.errorCode,
                            error.errorString
                        )
                    )
                    MegaError.API_EMASTERONLY -> continuation.failWithException(
                        NotMasterBusinessAccountException(
                            error.errorCode,
                            error.errorString
                        )
                    )
                    else -> continuation.failWithError(error)
                }
            }
        }

    override fun monitorUserUpdates() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull { it.users }
        .map { userUpdateMapper(it) }

    override suspend fun getNumUnreadUserAlerts(): Int = withContext(ioDispatcher) {
        megaApiGateway.getNumUnreadUserAlerts()
    }

    override suspend fun getSession(): String? =
        localStorageGateway.getUserCredentials()?.session

    override fun retryPendingConnections(disconnect: Boolean) {
        megaApiGateway.retryPendingConnections()
        megaChatApiGateway.retryPendingConnections(disconnect)
    }
}