package mega.privacy.android.app.interfaces

import mega.privacy.android.app.main.megachat.data.FileGalleryItem

/*
 * This interface is to define what methods should
 * implement when having ChatRoomToolbarBottomSheetDialogFragment
 */
/**
 * Interface for implementing a callback to be
 * invoked when the a option in the bottom panel is clicked.
 */
interface ChatRoomToolbarBottomSheetDialogActionListener {
    /**
     * Called when the take picture option is clicked.
     */
    fun onTakePictureOptionClicked()

    /**
     * Called when file is clicked.
     */
    fun onSendFilesSelected(files: ArrayList<FileGalleryItem>)

    /**
     * Called when the record voice clip option is clicked.
     */
    fun onRecordVoiceClipClicked()

    /**
     * Called when send file option is clicked.
     */
    fun onSendFileOptionClicked()

    /**
     * Called when stat call option is clicked.
     */
    fun onStartCallOptionClicked(videoOn: Boolean)

    /**
     * Called when scan document option is clicked.
     */
    fun onScanDocumentOptionClicked()

    /**
     * Called when send GIF option is clicked.
     */
    fun onSendGIFOptionClicked()

    /**
     * Called when the send location option is clicked.
     */
    fun onSendLocationOptionClicked()

    /**
     * Called when the send contact option is clicked.
     */
    fun onSendContactOptionClicked()
}