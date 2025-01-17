package mega.privacy.android.app.presentation.featureflag.model

import mega.privacy.android.domain.entity.FeatureFlag

/**
 * Data class for Feature flag menu UI state
 *
 * @param featureFlagList = List of @FeatureFlag
 */
data class FeatureFlagState(val featureFlagList: List<FeatureFlag> = emptyList())
