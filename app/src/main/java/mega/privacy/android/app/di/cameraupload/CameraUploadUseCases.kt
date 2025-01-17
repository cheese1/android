package mega.privacy.android.app.di.cameraupload

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.DefaultGetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.DefaultGetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.DefaultIsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.DefaultIsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultClearSyncRecords
import mega.privacy.android.domain.usecase.DefaultCompressedVideoPending
import mega.privacy.android.domain.usecase.DefaultGetSyncRecordByPath
import mega.privacy.android.domain.usecase.DefaultIsChargingRequired
import mega.privacy.android.domain.usecase.DefaultShouldCompressVideo
import mega.privacy.android.domain.usecase.DefaultUpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp

/**
 * Provides the use case implementation for camera upload
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraUploadUseCases {

    companion object {
        /**
         * Provide the HasCredentials implementation
         */
        @Provides
        fun provideHasCredentials(cameraUploadRepository: CameraUploadRepository): HasCredentials =
            HasCredentials(cameraUploadRepository::doCredentialsExist)

        /**
         * Provide the HasPreferences implementation
         */
        @Provides
        fun provideHasPreferences(cameraUploadRepository: CameraUploadRepository): HasPreferences =
            HasPreferences(cameraUploadRepository::doPreferencesExist)

        /**
         * Provide the IsCameraUploadSyncEnabled implementation
         */
        @Provides
        fun provideIsCameraUploadSyncEnabled(cameraUploadRepository: CameraUploadRepository): IsCameraUploadSyncEnabled =
            IsCameraUploadSyncEnabled(cameraUploadRepository::isSyncEnabled)

        /**
         * Provide the IsCameraUploadByWifi implementation
         */
        @Provides
        fun provideIsCameraUploadByWifi(cameraUploadRepository: CameraUploadRepository): IsCameraUploadByWifi =
            IsCameraUploadByWifi(cameraUploadRepository::isSyncByWifi)

        /**
         * Provide the GetChargingOnSizeString implementation
         */
        @Provides
        fun provideGetChargingOnSizeString(cameraUploadRepository: CameraUploadRepository): GetChargingOnSizeString =
            GetChargingOnSizeString(cameraUploadRepository::getChargingOnSizeString)

        /**
         * Provide the GetPendingSyncRecords implementation
         */
        @Provides
        fun provideGetPendingSyncRecords(cameraUploadRepository: CameraUploadRepository): GetPendingSyncRecords =
            GetPendingSyncRecords(cameraUploadRepository::getPendingSyncRecords)

        /**
         * Provide the MediaLocalPathExists implementation
         */
        @Provides
        fun provideMediaLocalPathExists(cameraUploadRepository: CameraUploadRepository): MediaLocalPathExists =
            MediaLocalPathExists { filePath, isSecondary ->
                cameraUploadRepository.doesLocalPathExist(filePath,
                    isSecondary,
                    SyncRecordType.TYPE_ANY.value)
            }

        /**
         * Provide the IsSecondaryFolderEnabled implementation
         */
        @Provides
        fun provideIsSecondaryFolderEnabled(cameraUploadRepository: CameraUploadRepository): IsSecondaryFolderEnabled =
            IsSecondaryFolderEnabled(cameraUploadRepository::isSecondaryMediaFolderEnabled)

        /**
         * Provide the DeleteSyncRecord implementation
         */
        @Provides
        fun provideDeleteSyncRecord(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecord =
            DeleteSyncRecord(cameraUploadRepository::deleteSyncRecord)

        /**
         * Provide the DeleteSyncRecordByLocalPath implementation
         */
        @Provides
        fun provideDeleteSyncRecordByLocalPath(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecordByLocalPath =
            DeleteSyncRecordByLocalPath(cameraUploadRepository::deleteSyncRecordByLocalPath)

        /**
         * Provide the DeleteSyncRecordByFingerprint implementation
         */
        @Provides
        fun provideDeleteSyncRecordByFingerprint(cameraUploadRepository: CameraUploadRepository): DeleteSyncRecordByFingerprint =
            DeleteSyncRecordByFingerprint(cameraUploadRepository::deleteSyncRecordByFingerprint)

        /**
         * Provide the SetSecondaryFolderPath implementation
         */
        @Provides
        fun provideSetSecondaryFolderPath(cameraUploadRepository: CameraUploadRepository): SetSecondaryFolderPath =
            SetSecondaryFolderPath(cameraUploadRepository::setSecondaryFolderPath)

        /**
         * Provide the SetSyncLocalPath implementation
         */
        @Provides
        fun provideSetSyncLocalPath(cameraUploadRepository: CameraUploadRepository): SetSyncLocalPath =
            SetSyncLocalPath(cameraUploadRepository::setSyncLocalPath)

        /**
         * Provide the GetRemoveGps implementation
         */
        @Provides
        fun provideGetRemoveGps(cameraUploadRepository: CameraUploadRepository): GetRemoveGps =
            GetRemoveGps(cameraUploadRepository::getRemoveGpsDefault)

        /**
         * Provide the FileNameExists implementation
         */
        @Provides
        fun provideFileNameExists(cameraUploadRepository: CameraUploadRepository): FileNameExists =
            FileNameExists { fileName, isSecondary ->
                cameraUploadRepository.doesFileNameExist(fileName,
                    isSecondary,
                    SyncRecordType.TYPE_ANY.value)
            }

        /**
         * Provide the KeepFileNames implementation
         */
        @Provides
        fun provideKeepFileNames(cameraUploadRepository: CameraUploadRepository): KeepFileNames =
            KeepFileNames(cameraUploadRepository::getKeepFileNames)

        /**
         * Provide the GetSyncRecordByFingerprint implementation
         */
        @Provides
        fun provideGetSyncRecordByFingerprint(cameraUploadRepository: CameraUploadRepository): GetSyncRecordByFingerprint =
            GetSyncRecordByFingerprint(cameraUploadRepository::getSyncRecordByFingerprint)

        /**
         * Provide the SaveSyncRecord implementation
         */
        @Provides
        fun provideSaveSyncRecord(cameraUploadRepository: CameraUploadRepository): SaveSyncRecord =
            SaveSyncRecord(cameraUploadRepository::saveSyncRecord)

        /**
         * Provide the SetSyncRecordPendingByPath implementation
         */
        @Provides
        fun provideSetSyncRecordPendingByPath(cameraUploadRepository: CameraUploadRepository): SetSyncRecordPendingByPath =
            SetSyncRecordPendingByPath { localPath, isSecondary ->
                cameraUploadRepository.updateSyncRecordStatusByLocalPath(SyncStatus.STATUS_PENDING.value,
                    localPath,
                    isSecondary)
            }

        /**
         * Provide the GetVideoSyncRecordsByStatus implementation
         */
        @Provides
        fun provideGetVideoSyncRecordsByStatus(cameraUploadRepository: CameraUploadRepository): GetVideoSyncRecordsByStatus =
            GetVideoSyncRecordsByStatus { cameraUploadRepository.getVideoSyncRecordsByStatus(it.value) }

        /**
         * Provide the GetVideoQuality implementation
         */
        @Provides
        fun provideGetVideoQuality(cameraUploadRepository: CameraUploadRepository): GetVideoQuality =
            GetVideoQuality { cameraUploadRepository.getVideoQuality().toInt() }
    }

    /**
     * Provide the UpdateTimeStamp implementation
     */
    @Binds
    abstract fun bindUpdateTimeStamp(updateTimeStamp: DefaultUpdateCameraUploadTimeStamp): UpdateCameraUploadTimeStamp

    /**
     * Provide the GetCameraUploadLocalPath implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPath(getLocalPath: DefaultGetCameraUploadLocalPath): GetCameraUploadLocalPath

    /**
     * Provide the GetCameraUploadLocalPathSecondary implementation
     */
    @Binds
    abstract fun bindGetCameraUploadLocalPathSecondary(getLocalPathSecondary: DefaultGetCameraUploadLocalPathSecondary): GetCameraUploadLocalPathSecondary

    /**
     * Provide the GetCameraUploadSelectionQuery implementation
     */
    @Binds
    abstract fun bindGetCameraUploadSelectionQuery(getSelectionQuery: DefaultGetCameraUploadSelectionQuery): GetCameraUploadSelectionQuery

    /**
     * Provide the IsLocalPrimaryFolderSet implementation
     */
    @Binds
    abstract fun bindIsLocalPrimaryFolderSet(isLocalPrimaryFolderSet: DefaultIsLocalPrimaryFolderSet): IsLocalPrimaryFolderSet

    /**
     * Provide the IsLocalSecondaryFolderSet implementation
     */
    @Binds
    abstract fun bindIsLocalSecondaryFolderSet(isLocalSecondaryFolderSet: DefaultIsLocalSecondaryFolderSet): IsLocalSecondaryFolderSet

    /**
     * Provide the IsWifiNotSatisfied implementation
     */
    @Binds
    abstract fun bindIsWifiNotSatisfied(isWifiNotSatisfied: DefaultIsWifiNotSatisfied): IsWifiNotSatisfied

    /**
     * Provide the GetSyncFileUploadUris implementation
     */
    @Binds
    abstract fun bindGetSyncFileUploadUris(getSyncFileUploadUris: DefaultGetSyncFileUploadUris): GetSyncFileUploadUris

    /**
     * Provide the ShouldCompressVideo implementation
     */
    @Binds
    abstract fun bindShouldCompressVideo(shouldCompressVideo: DefaultShouldCompressVideo): ShouldCompressVideo

    /**
     * Provide the GetSyncRecordByPath implementation
     */
    @Binds
    abstract fun bindGetSyncRecordByPath(getSyncRecordByPath: DefaultGetSyncRecordByPath): GetSyncRecordByPath

    /**
     * Provide the ClearSyncRecords implementation
     */
    @Binds
    abstract fun bindClearSyncRecords(clearSyncRecords: DefaultClearSyncRecords): ClearSyncRecords

    /**
     * Provide the CompressedVideoPending implementation
     */
    @Binds
    abstract fun bindCompressedVideoPending(compressedVideoPending: DefaultCompressedVideoPending): CompressedVideoPending

    /**
     * Provide the IsChargingRequired implementation
     */
    @Binds
    abstract fun bindIsChargingRequired(isChargingRequired: DefaultIsChargingRequired): IsChargingRequired
}
