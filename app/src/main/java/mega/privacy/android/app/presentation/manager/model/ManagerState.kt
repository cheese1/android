package mega.privacy.android.app.presentation.manager.model

/**
 * Manager UI state
 *
 * @param browserParentHandle current browser parent handle
 * @param rubbishBinParentHandle current rubbish bin parent handle
 * @param inboxParentHandle current inbox parent handle
 * @param isFirstNavigationLevel true if the navigation level is the first level
 * @param sharesTab current tab in shares screen
 * @param transfersTab current tab in transfers screen
 */
data class ManagerState(
    val browserParentHandle: Long = -1L,
    val rubbishBinParentHandle: Long = -1L,
    val inboxParentHandle: Long = -1L,
    val isFirstNavigationLevel: Boolean = true,
    var sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    var transfersTab: TransfersTab = TransfersTab.NONE,
    var isFirstLogin: Boolean = false
)