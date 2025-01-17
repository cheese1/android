package mega.privacy.android.domain.entity

/**
 * Data class to hold feature flag information
 *
 * @property featureName
 * @property isEnabled
 */
data class FeatureFlag(var featureName: String, var isEnabled: Boolean)
