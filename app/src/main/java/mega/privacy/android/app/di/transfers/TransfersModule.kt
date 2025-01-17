package mega.privacy.android.app.di.transfers

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.repository.TransfersRepository
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.domain.usecase.AreTransfersPaused
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.GetSizeTransferInfo
import mega.privacy.android.domain.usecase.IsCompletedTransfersEmpty

/**
 * Use cases to check on transfer status
 */
@Module
@InstallIn(ViewModelComponent::class)
class TransfersModule {

    @Provides
    fun provideAreTransfersPaused(transfersRepository: TransfersRepository):
            AreTransfersPaused = AreTransfersPaused(transfersRepository::areTransfersPaused)

    @Provides
    fun provideGetNumPendingDownloadsNonBackground(transfersRepository: TransfersRepository):
            GetNumPendingDownloadsNonBackground =
        GetNumPendingDownloadsNonBackground(transfersRepository::getNumPendingDownloadsNonBackground)

    @Provides
    fun provideGetNumPendingUploads(transfersRepository: TransfersRepository): GetNumPendingUploads =
        GetNumPendingUploads(transfersRepository::getNumPendingUploads)

    @Provides
    fun provideGetNumPendingTransfers(transfersRepository: TransfersRepository): GetNumPendingTransfers =
        GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

    @Provides
    fun provideIsCompletedTransfersEmpty(transfersRepository: TransfersRepository): IsCompletedTransfersEmpty =
        IsCompletedTransfersEmpty(transfersRepository::isCompletedTransfersEmpty)

    @Provides
    fun provideAreAllTransfersPaused(transfersRepository: TransfersRepository): AreAllTransfersPaused =
        AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

    /**
     * Provide get size transfer info
     */
    @Provides
    fun provideGetSizeTransferInfo(transfersRepository: TransfersRepository) =
        GetSizeTransferInfo(transfersRepository::getSizeTransfer)
}
