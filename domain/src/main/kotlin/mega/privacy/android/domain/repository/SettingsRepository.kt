package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.ChatImageQuality

/**
 * Settings repository - class for handling all calls relating to settings
 *
 */
interface SettingsRepository {

    /**
     * Is passcode lock preference enabled
     *
     * @return true if enabled
     */
    fun isPasscodeLockPreferenceEnabled(): Boolean

    /**
     * Set passcode lock enabled/disabled
     *
     * @param enabled
     */
    fun setPasscodeLockEnabled(enabled: Boolean)

    /**
     * Set the passcode lock code
     */
    suspend fun setPasscodeLockCode(passcodeLockCode: String)

    /**
     * Fetch contact links option
     *
     * @return true if option is enabled, else false
     */
    suspend fun fetchContactLinksOption(): Boolean

    /**
     * Get start screen
     *
     * @return start screen key
     */
    fun getStartScreen(): Int

    /**
     * Should hide recent activity
     *
     * @return true if option is enabled, else false
     */
    fun shouldHideRecentActivity(): Boolean

    /**
     * Set auto accept qr requests
     *
     * @param accept
     * @return true if option is enabled, else false
     */
    suspend fun setAutoAcceptQR(accept: Boolean): Boolean

    /**
     * Monitor start screen
     *
     * @return start screen key changes as a flow
     */
    fun monitorStartScreen(): Flow<Int>

    /**
     * Monitor hide recent activity
     *
     * @return hide recent activity option enabled status as a flow
     */
    fun monitorHideRecentActivity(): Flow<Boolean>

    /**
     * Is camera sync enabled
     *
     * @return
     */
    fun isCameraSyncPreferenceEnabled(): Boolean

    /**
     * Set camera upload
     */
    suspend fun setEnableCameraUpload(enable: Boolean)

    /**
     * Set camera upload video quality
     */
    suspend fun setCameraUploadVideoQuality(quality: Int)

    /**
     * Set if photos only or photos and videos
     */
    suspend fun setCameraUploadFileType(syncVideo: Boolean)

    /**
     * Set if use wifi only or cellular as well
     *
     * @param enableCellularSync
     */
    suspend fun setCamSyncWifi(enableCellularSync: Boolean)

    /**
     * Set the camera upload loca path
     */
    suspend fun setCameraUploadLocalPath(path: String?)

    /**
     * Set if camera folder is in external sd card
     */
    suspend fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean)

    /**
     * Set conversion on charging
     */
    suspend fun setConversionOnCharging(onCharging: Boolean)

    /**
     * Set conversion charging on size
     */
    suspend fun setChargingOnSize(size: Int)

    /**
     * Set to always ask for storage
     *
     * @param isStorageAskAlways
     */
    suspend fun setStorageAskAlways(isStorageAskAlways: Boolean)

    /**
     * Set the default storage download location
     */
    suspend fun setDefaultStorageDownloadLocation()

    /**
     * Set Storage download location
     *
     * @param storageDownloadLocation
     */
    suspend fun setStorageDownloadLocation(storageDownloadLocation: String)

    /**
     * Set if we want to show copyright notice
     *
     * @param showCopyrights
     */
    suspend fun setShowCopyright()

    /**
     * Is use https preference set
     *
     * @return true if set
     * @return true if set
     */
    suspend fun isUseHttpsPreferenceEnabled(): Boolean

    /**
     * Set use https preference
     *
     * @param enabled
     */
    suspend fun setUseHttpsPreference(enabled: Boolean)

    /**
     * Gets chat image quality.
     *
     * @return Chat image quality.
     */
    fun getChatImageQuality(): Flow<ChatImageQuality>

    /**
     * Sets chat image quality.
     *
     * @param quality New chat image quality.
     * @return Chat image quality.
     */
    suspend fun setChatImageQuality(quality: ChatImageQuality)

    /**
     * Gets if call notification sounds are enabled or disabled.
     *
     * @return Sound notifications status.
     */
    fun getCallsSoundNotifications(): Flow<CallsSoundNotifications>

    /**
     * Enabling or disabling call notification sounds.
     *
     * @param soundNotifications New Sound notifications status.
     * @return Sound notifications status.
     */
    suspend fun setCallsSoundNotifications(soundNotifications: CallsSoundNotifications)

    /**
     * Set string preference
     *
     * @param key
     * @param value
     */
    suspend fun setStringPreference(key: String?, value: String?)

    /**
     * Set string set preference
     *
     * @param key
     * @param value
     */
    suspend fun setStringSetPreference(key: String?, value: MutableSet<String>?)

    /**
     * Set int preference
     *
     * @param key
     * @param value
     */
    suspend fun setIntPreference(key: String?, value: Int?)

    /**
     * Set long preference
     *
     * @param key
     * @param value
     */
    suspend fun setLongPreference(key: String?, value: Long?)

    /**
     * Set float preference
     *
     * @param key
     * @param value
     */
    suspend fun setFloatPreference(key: String?, value: Float?)

    /**
     * Set boolean preference
     *
     * @param key
     * @param value
     */
    suspend fun setBooleanPreference(key: String?, value: Boolean?)

    /**
     * Monitor string preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorStringPreference(key: String?, defaultValue: String?): Flow<String?>

    /**
     * Monitor string set preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorStringSetPreference(
        key: String?,
        defaultValue: MutableSet<String>?,
    ): Flow<MutableSet<String>?>

    /**
     * Monitor int preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorIntPreference(key: String?, defaultValue: Int): Flow<Int>

    /**
     * Monitor long preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorLongPreference(key: String?, defaultValue: Long): Flow<Long>

    /**
     * Monitor float preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorFloatPreference(key: String?, defaultValue: Float): Flow<Float>

    /**
     * Monitor boolean preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorBooleanPreference(key: String?, defaultValue: Boolean): Flow<Boolean>

    /**
     * Get last contact permission dismissed time
     */
    fun getLastContactPermissionDismissedTime(): Flow<Long>

    /**
     * Set last contact permission dismissed time
     *
     * @param time
     */
    suspend fun setLastContactPermissionDismissedTime(time: Long)
}
