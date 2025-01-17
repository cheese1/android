package mega.privacy.android.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.screenshot.navigateToShowkase
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import java.io.File

@AndroidEntryPoint
class QASettingsFragment : PreferenceFragmentCompat() {
    val viewModel by viewModels<QASettingViewModel>()

    private val composeBrowserPreferenceKey = "settings_qa_compose_browser"
    private val checkForUpdatesPreferenceKey = "settings_qa_check_update"
    private val exportLogsPreferenceKey = "settings_qa_export_logs"
    private val saveLogsPreferenceKey = "settings_qa_save_logs"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            composeBrowserPreferenceKey -> {
                activity?.navigateToShowkase()
                true
            }
            checkForUpdatesPreferenceKey -> {
                viewModel.checkUpdatePressed()
                true
            }
            exportLogsPreferenceKey -> {
                viewModel.exportLogs(::sendShareLogFileIntent)
                true
            }
            saveLogsPreferenceKey -> {
                viewModel.exportLogs(::sendSaveLogFileIntent)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun sendShareLogFileIntent(logFile: File) = Intent(Intent.ACTION_SEND).apply {
        type = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(logFile.extension)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile))
        putExtra(Intent.EXTRA_TITLE, "Send log file")
        putExtra(Intent.EXTRA_SUBJECT, "Mega Log")
    }.let {
        startActivity(Intent.createChooser(it, null))
    }

    private fun sendSaveLogFileIntent(logFile: File) {
        val data = FileProvider.getUriForFile(requireContext(),
            AUTHORITY_STRING_FILE_PROVIDER, logFile)
        val type = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(logFile.extension)
        requireContext().grantUriPermission(requireContext().packageName,
            data,
            Intent.FLAG_GRANT_READ_URI_PERMISSION)

        Intent(Intent.ACTION_VIEW)
            .setDataAndType(data, type)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .let {
                startActivity(it)
            }

    }
}