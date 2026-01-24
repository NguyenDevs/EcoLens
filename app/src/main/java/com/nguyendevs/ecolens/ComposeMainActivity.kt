package com.nguyendevs.ecolens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.ui.components.FullScreenImageViewer
import com.nguyendevs.ecolens.ui.screens.main.MainScreen
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.view.EcoLensViewModel

/** Main Activity using Jetpack Compose. Replaces MainActivity for full Compose experience. */
class ComposeMainActivity : ComponentActivity() {

    private lateinit var viewModel: EcoLensViewModel
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREF_NAME = "EcoLensPrefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"

        fun newIntent(context: Context): Intent {
            return Intent(context, ComposeMainActivity::class.java)
        }
    }

    private val cameraActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
                    if (uriString != null) {
                        handleCapturedImage(uriString.toUri())
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        viewModel = ViewModelProvider(this)[EcoLensViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            var isDarkMode by remember {
                mutableStateOf(sharedPreferences.getBoolean(KEY_DARK_MODE, false))
            }
            var showFullScreenImage by remember { mutableStateOf(false) }
            var isSpeaking by remember { mutableStateOf(false) }

            val currentLanguage = sharedPreferences.getString(KEY_LANGUAGE, "vi") ?: "vi"
            val languageDisplay = if (currentLanguage == "vi") "Tiếng Việt" else "English"

            EcoLensTheme(darkTheme = isDarkMode) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                            // Home screen state
                            isHomeExpanded = uiState.imageUri != null,
                            isLoading = uiState.isLoading,
                            loadingText = uiState.loadingText,
                            error = uiState.error,
                            imageUri = uiState.imageUri,
                            speciesInfo = uiState.speciesInfo,

                            // Settings state
                            currentLanguage = languageDisplay,
                            isDarkMode = isDarkMode,
                            isLoggedIn = true, // TODO: Connect to auth state

                            // Speaker state
                            isSpeaking = isSpeaking,
                            showSpeakerButton = uiState.speciesInfo != null,

                            // Callbacks
                            onCameraClick = { openCamera() },
                            onZoomClick = { showFullScreenImage = true },
                            onCopyScientificName = { name -> copyToClipboard(name) },
                            onRetryClick = { viewModel.retry() },
                            onSpeakClick = {
                                isSpeaking = true
                                // TODO: Start TTS
                            },
                            onMuteClick = {
                                isSpeaking = false
                                // TODO: Stop TTS
                            },

                            // Settings callbacks
                            onLanguageClick = { /* TODO: Show language dialog */},
                            onDarkModeToggle = { enabled ->
                                isDarkMode = enabled
                                sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
                            },
                            onFeedbackClick = { sendFeedbackEmail() },
                            onFacebookClick = { openUrl("https://facebook.com/nguyendevs") },
                            onInstagramClick = { openUrl("https://instagram.com/nguyendevs") },
                            onTiktokClick = { openUrl("https://tiktok.com/@nguyendevs") },
                            onAboutClick = { /* TODO: Show about dialog */},
                            onChangeUsernameClick = { /* TODO: Change username */},
                            onChangePasswordClick = { /* TODO: Change password */},
                            onLinkGoogleClick = { /* TODO: Link Google */},
                            onDeleteAccountClick = { /* TODO: Delete account */},
                            onLogoutClick = { /* TODO: Logout */},

                            // History/Chat content
                            historyContent = {
                                // Placeholder - will be replaced with actual data
                                com.nguyendevs.ecolens.ui.screens.history.HistoryScreen(
                                        items = emptyList(),
                                        onItemClick = { /* TODO */},
                                        onDeleteClick = { /* TODO */}
                                )
                            },
                            chatContent = {
                                // Placeholder - will be replaced with actual data
                                com.nguyendevs.ecolens.ui.screens.chat.ChatHistoryScreen(
                                        items = emptyList(),
                                        onItemClick = { /* TODO */},
                                        onDeleteClick = { /* TODO */},
                                        onNewChatClick = { /* TODO */}
                                )
                            }
                    )

                    // Full screen image viewer
                    FullScreenImageViewer(
                            imageUri = uiState.imageUri,
                            visible = showFullScreenImage,
                            onCloseClick = { showFullScreenImage = false }
                    )
                }
            }
        }
    }

    private fun openCamera() {
        val intent = CameraActivity.newIntent(this)
        cameraActivityLauncher.launch(intent)
    }

    private fun handleCapturedImage(uri: Uri) {
        viewModel.analyzeImage(uri)
    }

    private fun copyToClipboard(text: String) {
        val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Scientific Name", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(this, "Đã sao chép: $text", android.widget.Toast.LENGTH_SHORT)
                .show()
    }

    private fun sendFeedbackEmail() {
        val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@nguyendevs.com")
                    putExtra(Intent.EXTRA_SUBJECT, "EcoLens Feedback")
                }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                            this,
                            getString(R.string.error_no_email_app),
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                            this,
                            getString(R.string.error_open_link),
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }
}
