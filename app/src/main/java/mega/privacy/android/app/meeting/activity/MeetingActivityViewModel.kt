package mega.privacy.android.app.meeting.activity

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_AUDIO_OUTPUT_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_TITLE_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LINK_RECOVERED
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_CREATED
import mega.privacy.android.app.constants.EventConstants.EVENT_NETWORK_CHANGE
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.InviteToChatRoomListener
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.meeting.listeners.OpenVideoDeviceListener
import mega.privacy.android.app.usecase.call.AnswerCallUseCase
import mega.privacy.android.app.usecase.call.GetCallUseCase
import mega.privacy.android.app.usecase.call.GetLocalAudioChangesUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.amIParticipatingInAChat
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING
import mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject

/**
 * It's very common that two or more fragments in Meeting activity need to communicate with each other.
 * These fragments can share a ViewModel using their activity scope to handle this communication.
 * MeetingActivityViewModel shares state of Mic, Camera and Speaker for all Fragments
 */
@HiltViewModel
class MeetingActivityViewModel @Inject constructor(
    private val meetingActivityRepository: MeetingActivityRepository,
    private val answerCallUseCase: AnswerCallUseCase,
    getLocalAudioChangesUseCase: GetLocalAudioChangesUseCase,
    private val getCallUseCase: GetCallUseCase,
) : BaseRxViewModel(), OpenVideoDeviceListener.OnOpenVideoDeviceCallback,
    DisableAudioVideoCallListener.OnDisableAudioVideoCallback {

    // Avatar
    private val _avatarLiveData = MutableLiveData<Bitmap>()
    val avatarLiveData: LiveData<Bitmap> = _avatarLiveData

    var tips: MutableLiveData<String> = MutableLiveData<String>()
    var micLocked: Boolean = false

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _cameraLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _speakerLiveData: MutableLiveData<AppRTCAudioManager.AudioDevice> =
        MutableLiveData<AppRTCAudioManager.AudioDevice>().apply {
            value = if (MegaApplication.getInstance().audioManager == null) {
                AppRTCAudioManager.AudioDevice.NONE
            } else {
                MegaApplication.getInstance().audioManager!!.selectedAudioDevice
            }
        }

    val micLiveData: LiveData<Boolean> = _micLiveData
    val cameraLiveData: LiveData<Boolean> = _cameraLiveData
    val speakerLiveData: LiveData<AppRTCAudioManager.AudioDevice> = _speakerLiveData

    // Permissions
    private val _cameraGranted = MutableStateFlow(false)
    val cameraGranted: StateFlow<Boolean> get() = _cameraGranted
    private val _recordAudioGranted = MutableStateFlow(false)
    val recordAudioGranted: StateFlow<Boolean> get() = _recordAudioGranted

    private val _cameraPermissionCheck = MutableLiveData<Boolean>()
    val cameraPermissionCheck: LiveData<Boolean> = _cameraPermissionCheck

    private val _recordAudioPermissionCheck = MutableLiveData<Boolean>()
    val recordAudioPermissionCheck: LiveData<Boolean> = _recordAudioPermissionCheck

    // Network State
    private val _notificationNetworkState = MutableLiveData<Boolean>()

    // Observe this property to get online/offline notification. true: online / false: offline
    val notificationNetworkState: LiveData<Boolean> = _notificationNetworkState

    private val notificationNetworkStateObserver = Observer<Boolean> {
        _notificationNetworkState.value = it
    }

    private val _currentChatId: MutableLiveData<Long> =
        MutableLiveData<Long>(MEGACHAT_INVALID_HANDLE)
    val currentChatId: LiveData<Long> = _currentChatId

    // Name of meeting
    private val _meetingNameLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingNameLiveData: LiveData<String> = _meetingNameLiveData

    // Link of meeting
    private val _meetingLinkLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingLinkLiveData: LiveData<String> = _meetingLinkLiveData

    // Show snack bar
    private val _snackBarLiveData = MutableLiveData("")
    val snackBarLiveData: LiveData<String> = _snackBarLiveData

    //Control when call should be switched
    private val _switchCall = MutableStateFlow(MEGACHAT_INVALID_HANDLE)
    val switchCall: StateFlow<Long> get() = _switchCall

    //Control when call should be finish
    private val _finishMeetingActivity = MutableStateFlow(false)
    val finishMeetingActivity: StateFlow<Boolean> get() = _finishMeetingActivity

    private val audioOutputStateObserver =
        Observer<AppRTCAudioManager.AudioDevice> {
            if (_speakerLiveData.value != it && it != AppRTCAudioManager.AudioDevice.NONE) {
                Timber.d("Updating speaker $it")

                _speakerLiveData.value = it
                tips.value = when (it) {
                    AppRTCAudioManager.AudioDevice.EARPIECE -> getString(R.string.general_speaker_off)
                    AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> getString(R.string.general_speaker_on)
                    else -> getString(R.string.general_headphone_on)
                }
            }
        }

    private val meetingCreatedObserver =
        Observer<Long> {
            updateChatRoomId(it)
            MegaApplication.setIsWaitingForCall(true)
            createChatLink(it)
        }

    private val linkRecoveredObserver =
        Observer<android.util.Pair<Long, String>> { chatAndLink ->
            _currentChatId.value?.let {
                if (chatAndLink.first == it) {
                    if (!chatAndLink.second.isNullOrEmpty()) {
                        _meetingLinkLiveData.value = chatAndLink.second
                    } else {
                        _snackBarLiveData.value = getString(R.string.no_chat_link_available)
                    }
                }
            }
        }

    private val titleMeetingChangeObserver =
        Observer<MegaChatRoom> { chatRoom ->
            meetingActivityRepository.getChatRoom(_currentChatId.value!!)?.let {
                if (it.chatId == chatRoom.chatId) {
                    _meetingNameLiveData.value = getTitleChat(it)
                }
            }
        }

    init {
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .observeForever(notificationNetworkStateObserver)

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .observeForever(audioOutputStateObserver)

        LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java)
            .observeForever(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .observeForever(meetingCreatedObserver)

        getCallUseCase.getCallEnded()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { chatIdOfCallEnded ->
                    currentChatId.value.let { currentChatId ->
                        if (chatIdOfCallEnded == currentChatId) {
                            _finishMeetingActivity.value = true
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        getLocalAudioChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { call ->
                    currentChatId.value?.let {
                        if (call.chatid == it) {
                            val isEnable = call.hasLocalAudio()
                            _micLiveData.value = isEnable
                            Timber.d("open Mic: $isEnable")
                            tips.value = when (isEnable) {
                                true -> getString(
                                    R.string.general_mic_unmute
                                )
                                false -> getString(
                                    R.string.general_mic_mute
                                )
                            }
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_LINK_RECOVERED)
            .observeForever(linkRecoveredObserver as Observer<Any>)

        // Show the default avatar (the Alphabet avatar) above all, then load the actual avatar
        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    /**
     * Control when calls are to be switched
     */
    fun clickSwitchCall() {
        checkAnotherCalls(false)
    }

    /**
     * Control when call should be finish
     */
    fun clickEndCall() {
        checkAnotherCalls(true)
    }

    /**
     * Determine if I am a guest
     *
     * @return True, if I am a guest. False if not
     */
    fun amIAGuest(): Boolean = meetingActivityRepository.amIAGuest()

    /**
     * Log out the Ephemeral Account PlusPlus
     *
     * @param meetingActivity Context
     */
    fun logOutTheGuest(meetingActivity: Context) {
        AccountController.logout(
            meetingActivity,
            MegaApplication.getInstance().megaApi,
            viewModelScope
        )
    }

    /**
     * Check concurrent calls to see if the call should be switched or ended
     *
     * @param shouldEndCurrentCall if the current call should be finish
     */
    private fun checkAnotherCalls(shouldEndCurrentCall: Boolean) {
        currentChatId.value?.let { currentChatId ->
            val chatId =
                getCallUseCase.getChatIdOfAnotherCallInProgress(currentChatId).blockingGet()
            if (chatId != MEGACHAT_INVALID_HANDLE && chatId != currentChatId && _switchCall.value != chatId) {
                _switchCall.value = chatId
            } else if (shouldEndCurrentCall) {
                _finishMeetingActivity.value = true
            }
        }
    }

    /**
     * Show the default avatar (the Alphabet avatar)
     */
    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatarLiveData.value = meetingActivityRepository.getDefaultAvatar()
    }

    /**
     * Method to get a specific chat
     *
     * @param chatId Chat ID
     * @return MegaChatRoom
     */
    fun getSpecificChat(chatId: Long): MegaChatRoom? = meetingActivityRepository.getChatRoom(chatId)

    /**
     * Generate and show the round avatar based on the actual avatar stored in the cache folder.
     * Try to retrieve the avatar from the server if it has not been cached.
     * Showing the default avatar if the retrieve failed
     */
    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            meetingActivityRepository.loadAvatar()?.also {
                when {
                    it.first -> _avatarLiveData.value = it.second
                    retry -> meetingActivityRepository.createAvatar(object :
                        BaseListener(MegaApplication.getInstance()) {
                        override fun onRequestFinish(
                            api: MegaApiJava,
                            request: MegaRequest,
                            e: MegaError,
                        ) {
                            if (request.type == MegaRequest.TYPE_GET_ATTR_USER
                                && request.paramType == MegaApiJava.USER_ATTR_AVATAR
                                && e.errorCode == MegaError.API_OK
                            ) {
                                loadAvatar()
                            } else {
                                showDefaultAvatar()
                            }
                        }
                    })
                    else -> {
                        showDefaultAvatar()
                    }
                }
            }
        }
    }

    /**
     * Method for creating a chat link
     *
     * @param chatId chat ID
     */
    fun createChatLink(chatId: Long, isModerator: Boolean = true) {
        //The chat doesn't exist
        if (isModerator) {
            meetingActivityRepository.createChatLink(
                chatId,
                CreateGroupChatWithPublicLink()
            )
        } else {
            meetingActivityRepository.queryChatLink(
                chatId,
                CreateGroupChatWithPublicLink()
            )
        }
    }

    /**
     * Method for update the chatRoom ID
     *
     * @param chatId chat ID
     */
    fun updateChatRoomId(chatId: Long) {
        if (_currentChatId.value != chatId) {
            _currentChatId.value = chatId
        }
    }

    /**
     * Method to know if the chat exists, I am joined to the chat and the call
     *
     * @return True, if it exists. False, otherwise
     */
    fun isChatCreatedAndIParticipating(): Boolean =
        (_currentChatId.value != MEGACHAT_INVALID_HANDLE &&
                amIParticipatingInAChat(_currentChatId.value!!) &&
                CallUtil.amIParticipatingInThisMeeting(_currentChatId.value!!))

    /**
     * Method to initiate the call with the microphone on
     */
    fun micInitiallyOn() {
        Timber.d("Call with audio activated initially")
        _micLiveData.value = true
    }

    /**
     * Method to initiate the call with the camera on
     */
    fun camInitiallyOn() {
        Timber.d("Call with video activated initially")
        _cameraLiveData.value = true
    }

    /**
     * Method for setting a name for the meeting
     *
     * @param name The name
     */
    fun setMeetingsName(name: String) {
        _meetingNameLiveData.value = name
    }

    /**
     * Method for setting a title for the meeting
     *
     * @return The name
     */
    fun getMeetingName(): String? {
        return _meetingNameLiveData.value
    }

    /**
     * Method of locking the microphone button
     */
    fun lockMic() {
        micLocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Response of clicking mic fab
     *
     * @param shouldAudioBeEnabled True, if audio should be enabled. False, otherwise
     */
    fun clickMic(shouldAudioBeEnabled: Boolean) {
        if (micLocked) {
            return
        }
        // Check audio permission. If haven't been granted, ask for the permission and return
        if (!_recordAudioGranted.value) {
            _recordAudioPermissionCheck.value = true
            return
        }

        if (isChatCreatedAndIParticipating()) {
            meetingActivityRepository.switchMic(
                _currentChatId.value!!,
                shouldAudioBeEnabled,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        } else {
            //The chat is not yet created or the call is not yet established
            _micLiveData.value = shouldAudioBeEnabled
            Timber.d("open Mic: $shouldAudioBeEnabled")
            tips.value = if (shouldAudioBeEnabled) {
                getString(R.string.general_mic_unmute)
            } else {
                getString(R.string.general_mic_mute)
            }
        }
    }

    /**
     * Response of clicking camera Fab
     *
     * @param shouldVideoBeEnabled True, if video should be enabled. False, otherwise
     */
    fun clickCamera(shouldVideoBeEnabled: Boolean) {
        //Check camera permission. If haven't been granted, ask for the permission and return
        if (!_cameraGranted.value) {
            _cameraPermissionCheck.value = true
            return
        }

        if (isChatCreatedAndIParticipating()) {
            Timber.d("Clicked cam with chat")
            meetingActivityRepository.switchCamera(
                _currentChatId.value!!,
                shouldVideoBeEnabled,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        } else {
            Timber.d("Clicked cam without chat")
            //The chat is not yet created or the call is not yet established
            meetingActivityRepository.switchCameraBeforeStartMeeting(
                shouldVideoBeEnabled,
                OpenVideoDeviceListener(MegaApplication.getInstance(), this)
            )
        }
    }

    /**
     * Method to release the local video device because of the chat is not yet created
     * or the call is not yet established
     */
    fun releaseVideoDevice() {
        meetingActivityRepository.switchCameraBeforeStartMeeting(
            false,
            OpenVideoDeviceListener(MegaApplication.getInstance())
        )
    }

    /**
     * init RTC Audio Manager
     */
    fun initRTCAudioManager() {
        MegaApplication.getInstance()
            .createOrUpdateAudioManager(true, AUDIO_MANAGER_CREATING_JOINING_MEETING)
    }

    /**
     * Response of clicking Speaker Fab
     */
    fun clickSpeaker() {
        when (_speakerLiveData.value) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                Timber.d("Trying to switch to EARPIECE")
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.EARPIECE)
            }
            else -> {
                Timber.d("Trying to switch to SPEAKER_PHONE")
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }
        }
    }

    /**
     * Set camera permission
     *
     * @param cameraPermission true: the permission is granted
     */
    fun setCameraPermission(cameraPermission: Boolean) {
        if (_cameraGranted.value == cameraPermission)
            return

        _cameraGranted.value = cameraPermission
    }

    /**
     * Set record & audio permission
     *
     * @param recordAudioPermission true: the permission is granted
     */
    fun setRecordAudioPermission(recordAudioPermission: Boolean) {
        if (_recordAudioGranted.value == recordAudioPermission)
            return

        _recordAudioGranted.value = recordAudioPermission
    }

    /**
     * Method of obtaining the video
     *
     * @param chatId chatId
     * @param listener IndividualCallVideoListener
     */
    fun addLocalVideo(chatId: Long, listener: IndividualCallVideoListener?) {
        if (listener == null)
            return

        Timber.d("Adding local video")
        meetingActivityRepository.addLocalVideo(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener IndividualCallVideoListener
     */
    fun removeLocalVideo(chatId: Long, listener: IndividualCallVideoListener?) {
        if (listener == null) {
            Timber.e("Listener is null")
            return
        }

        Timber.d("Removing local video")
        meetingActivityRepository.removeLocalVideo(chatId, listener)
    }

    /**
     *  Select the video device to be used in calls
     *
     *  @param listener Receive information about requests.
     */
    fun setChatVideoInDevice(listener: MegaChatRequestListenerInterface?) {
        // Always try to start the video using the front camera
        val cameraDevice = VideoCaptureUtils.getFrontCamera()
        if (cameraDevice != null) {
            meetingActivityRepository.setChatVideoInDevice(cameraDevice, listener)
        }
    }

    /**
     * Method to update the status of the local camera and display the corresponding tips
     *
     * @param isVideoOn True, if the video is ON. False, otherwise
     */
    private fun updateCameraValueAndTips(isVideoOn: Boolean) {
        _cameraLiveData.value = isVideoOn
        Timber.d("Open video: ${_cameraLiveData.value}")
        tips.value = when (isVideoOn) {
            true -> getString(
                R.string.general_camera_enable
            )
            false -> getString(
                R.string.general_camera_disable
            )
        }
    }

    override fun onVideoDeviceOpened(isVideoOn: Boolean) {
        updateCameraValueAndTips(isVideoOn)
    }

    override fun onDisableAudioVideo(chatId: Long, typeChange: Int, isEnable: Boolean) {
        when (typeChange) {
            MegaChatRequest.VIDEO -> {
                updateCameraValueAndTips(isEnable)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .removeObserver(audioOutputStateObserver)

        // Remove observer on network state
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .removeObserver(notificationNetworkStateObserver)

        LiveEventBus.get(
            EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java
        ).removeObserver(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .removeObserver(meetingCreatedObserver)

        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_LINK_RECOVERED)
            .removeObserver(linkRecoveredObserver as Observer<Any>)
    }

    fun inviteToChat(context: Context, requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Result Code: $resultCode")
        if (intent == null) {
            Timber.w("Intent is null")
            return
        }
        if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            Timber.d("Participants successfully added")
            val contactsData: List<String>? =
                intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
            if (contactsData != null) {
                currentChatId.value?.let {
                    InviteToChatRoomListener(context).inviteToChat(it, contactsData)
                    _snackBarLiveData.value = getString(R.string.invite_sent)
                }
            }
        } else {
            Timber.e("Error adding participants")
        }
    }

    /**
     * Show snack bar
     *
     * @param content the content should be shown
     */
    fun showSnackBar(content: String) {
        _snackBarLiveData.value = content
    }

    /**
     * Hide snack bar
     */
    fun hideSnackBar() {
        _snackBarLiveData.value = ""
    }

    /**
     * Method for obtaining the bitmap of a participant's avatar
     *
     * @param peerId User handle of a participant
     * @return The bitmap of a participant's avatar
     */
    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        return meetingActivityRepository.getAvatarBitmapByPeerId(peerId)
    }

    /**
     * Change permissions to a call participant.
     *
     * @param userHandle User handle of a participant
     * @param permission type of permit to be assigned to the participant
     * @param listener MegaChatRequestListenerInterface
     */
    fun changeParticipantPermissions(
        userHandle: Long,
        permission: Int,
        listener: MegaChatRequestListenerInterface? = null,
    ) {
        currentChatId.value?.let {
            meetingActivityRepository.changeParticipantPermissions(it,
                userHandle,
                permission,
                listener)
        }
    }

    /**
     * Answer chat call
     *
     * @param enableVideo The video should be enabled
     * @param enableAudio The audio should be enabled
     * @param speakerAudio The speaker should be enabled
     * @return Result of the call
     */
    fun answerCall(
        enableVideo: Boolean,
        enableAudio: Boolean,
        speakerAudio: Boolean,
    ): LiveData<AnswerCallUseCase.AnswerCallResult> {
        val result = MutableLiveData<AnswerCallUseCase.AnswerCallResult>()
        _currentChatId.value?.let {
            answerCallUseCase.answerCall(it, enableVideo, enableAudio, speakerAudio)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { resultAnswerCall ->
                        result.value = resultAnswerCall
                    },
                    onError = { error ->
                        _finishMeetingActivity.value = true
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(composite)
        }
        return result
    }
}