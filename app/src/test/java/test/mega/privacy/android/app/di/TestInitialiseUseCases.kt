package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.InitialiseUseCases
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.RootNodeExists
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class, SingletonComponent::class],
    replaces = [InitialiseUseCases::class]
)
@Module
object TestInitialiseUseCases {

    val monitorConnectivity = mock<MonitorConnectivity> { on { invoke() }.thenReturn(flowOf(true)) }
    val rootNodeExists = mock<RootNodeExists> { on { runBlocking { invoke() } }.thenReturn(true) }

    @Provides
    fun bindMonitorConnectivity(): MonitorConnectivity = monitorConnectivity

    @Provides
    fun bindRootNodeExists(): RootNodeExists = rootNodeExists
}