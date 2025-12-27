// ComposeMainActivity.kt
package com.nguyendevs.ecolens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.managers.PermissionManager
import com.nguyendevs.ecolens.ui.navigation.EcoLensNavHost
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.view.EcoLensViewModel

class ComposeMainActivity : ComponentActivity() {

    private lateinit var languageManager: LanguageManager
    private lateinit var permissionManager: PermissionManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            permissionManager.showPermissionDeniedDialog()
        }
    }

    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                // Handle captured image
                // viewModel.identifySpecies(Uri.parse(uriString), languageManager.getLanguage())
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this, permissionLauncher)

        setContent {
            EcoLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcoLensApp(
                        onNavigateToCamera = ::handleCameraNavigation
                    )
                }
            }
        }
    }

    private fun handleCameraNavigation() {
        if (permissionManager.hasPermissions()) {
            cameraActivityLauncher.launch(CameraActivity.newIntent(this))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
        } else {
            permissionManager.requestPermissions()
        }
    }
}

@Composable
private fun EcoLensApp(
    onNavigateToCamera: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: EcoLensViewModel = viewModel()

    EcoLensNavHost(
        navController = navController,
        viewModel = viewModel,
        onNavigateToCamera = onNavigateToCamera
    )
}
