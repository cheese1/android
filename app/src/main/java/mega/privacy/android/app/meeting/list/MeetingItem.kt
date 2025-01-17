package mega.privacy.android.app.meeting.list

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.contacts.group.data.ContactGroupUser

/**
 * Meeting item
 *
 * @property chatId             Chat identifier
 * @property title              Chat title
 * @property lastMessage        Last chat message
 * @property isPublic           Check if chat is public
 * @property isMuted            Check if chat is muted
 * @property hasPermissions     Check if has permissions to clear history
 * @property firstUser          First user of the chat
 * @property lastUser           Last user of the chat
 * @property unreadCount        Unread messages count
 * @property highlight          Check if message should be highlighted
 * @property timeStamp          Last timestamp of the chat
 * @property formattedTimestamp Last timestamp of the chat formatted
 */
data class MeetingItem constructor(
    val chatId: Long,
    val title: String,
    val lastMessage: String?,
    val isPublic: Boolean,
    val isMuted: Boolean,
    val hasPermissions: Boolean,
    val firstUser: ContactGroupUser,
    val lastUser: ContactGroupUser?,
    val unreadCount: Int,
    val highlight: Boolean,
    val timeStamp: Long,
    val formattedTimestamp: String,
) {

    /**
     * Check if meeting contains only 1 user and myself
     *
     * @return  true if its single false otherwise
     */
    fun isSingleMeeting(): Boolean =
        lastUser == null

    class DiffCallback : DiffUtil.ItemCallback<MeetingItem>() {
        override fun areItemsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
            oldItem == newItem
    }
}
