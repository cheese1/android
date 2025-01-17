package mega.privacy.android.domain.usecase

/**
 * Use case to check if preferences exist
 */
fun interface HasPreferences {

    /**
     * Invoke
     *
     * @return do preferences exist
     */
    suspend operator fun invoke(): Boolean
}
