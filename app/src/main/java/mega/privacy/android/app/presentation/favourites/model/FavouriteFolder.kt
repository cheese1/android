package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nz.mega.sdk.MegaNode

/**
 * The favourite Folder entity
 */
data class FavouriteFolder(
    override val handle: Long,
    @DrawableRes override val icon: Int,
    override val name: String,
    @ColorRes override val labelColour: Int,
    override val showLabel: Boolean,
    override val node: MegaNode,
    override val hasVersion: Boolean,
    override val info: String,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
    override val isAvailableOffline: Boolean,
    override val isSelected: Boolean = false,
    override val label: Int,
) : Favourite {
    override val isFolder = true
    override val size: Long
        get() = 0L
    override val modificationTime: Long
        get() = 0L
    override val thumbnailPath: String? = null
}