package mega.privacy.android.app.presentation.settings.startSceen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentStartScreenSettingsBinding
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.PHOTOS_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.CHAT_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.CLOUD_DRIVE_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.HOME_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.SHARED_ITEMS_BNV
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Settings fragment to choose the preferred start screen.
 */
class StartScreenSettingsFragment : Fragment() {

    private val viewModel by activityViewModels<StartScreenViewModel>()

    private lateinit var binding: FragmentStartScreenSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartScreenSettingsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initPreferences(
            requireContext().getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
        )

        setupView()
        setupObservers()
    }

    private fun setupView() {
        hideChecks()

        binding.cloudLayout.setOnClickListener { viewModel.newScreenClicked(StartScreen.CloudDrive) }
        binding.cuLayout.setOnClickListener { viewModel.newScreenClicked(StartScreen.Photos) }
        binding.homeLayout.setOnClickListener { viewModel.newScreenClicked(StartScreen.Home) }
        binding.chatLayout.setOnClickListener { viewModel.newScreenClicked(StartScreen.Chat) }
        binding.sharedLayout.setOnClickListener { viewModel.newScreenClicked(StartScreen.SharedItems) }
    }

    private fun setupObservers() {
        viewModel.onScreenChecked().observe(viewLifecycleOwner, ::setScreenChecked)
    }

    private fun hideChecks() {
        binding.cloudCheck.isVisible = false
        binding.cuCheck.isVisible = false
        binding.homeCheck.isVisible = false
        binding.chatCheck.isVisible = false
        binding.sharedCheck.isVisible = false
    }

    /**
     * Updates the screen checked.
     *
     * @param screenChecked New screen checked.
     */
    private fun setScreenChecked(screenChecked: StartScreen) {
        hideChecks()

        when (screenChecked) {
            StartScreen.CloudDrive -> binding.cloudCheck.isVisible = true
            StartScreen.Photos -> binding.cuCheck.isVisible = true
            StartScreen.Home -> binding.homeCheck.isVisible = true
            StartScreen.Chat -> binding.chatCheck.isVisible = true
            StartScreen.SharedItems -> binding.sharedCheck.isVisible = true
            StartScreen.None -> {}
        }
    }
}