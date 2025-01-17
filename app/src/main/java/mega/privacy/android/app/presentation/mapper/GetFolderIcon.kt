package mega.privacy.android.app.presentation.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.BackupType
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderNode

/**
 * Get folder icon drawable
 */
@DrawableRes
internal fun getFolderIcon(folder: FolderNode) = with(folder) {
    when {
        isInRubbishBin -> R.drawable.ic_folder_list
        isIncomingShare -> R.drawable.ic_folder_incoming
        isMediaSyncFolder -> R.drawable.ic_folder_camera_uploads_list
        isChatFilesFolder -> R.drawable.ic_folder_chat_list
        isShared || folder.isPendingShare -> R.drawable.ic_folder_outgoing
        backupType == BackupType.Root -> R.drawable.backup
        backupType is BackupType.Device -> getDeviceFolderIcon(backupType as BackupType.Device)
        backupType == BackupType.Child -> R.drawable.ic_folder_backup
        else -> R.drawable.ic_folder_list
    }
}

private fun getDeviceFolderIcon(backupType: BackupType.Device) = when (backupType.deviceType) {
    DeviceType.Windows -> R.drawable.pc_win
    DeviceType.Linux -> R.drawable.pc_linux
    DeviceType.ExternalDrive -> R.drawable.ex_drive
    DeviceType.Mac -> R.drawable.pc_mac
    DeviceType.Unknown -> R.drawable.pc
}