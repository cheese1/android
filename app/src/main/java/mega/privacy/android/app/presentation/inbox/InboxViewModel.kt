package mega.privacy.android.app.presentation.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class associated to InboxFragment
 *
 * @param monitorNodeUpdates Monitor global node updates
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    /**
     * Monitor global node updates
     */
    val updateNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate()") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()
}