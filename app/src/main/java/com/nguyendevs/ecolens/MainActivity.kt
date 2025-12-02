package com.nguyendevs.ecolens

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.activity.CameraActivity
import com.nguyendevs.ecolens.activity.HistoryDetailFragment
import com.nguyendevs.ecolens.adapter.HistoryAdapter
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.manager.NavigationManager
import com.nguyendevs.ecolens.manager.PermissionManager
import com.nguyendevs.ecolens.handler.SpeciesInfoHandler
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Containers
    private lateinit var homeContainer: View
    private lateinit var historyContainer: View
    private lateinit var myGardenContainer: View
    private lateinit var settingsContainer: View

    // Home screen views
    private lateinit var fabSearch: FloatingActionButton
    private lateinit var imagePreview: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var loadingCard: MaterialCardView
    private lateinit var errorCard: MaterialCardView
    private lateinit var errorText: TextView
    private lateinit var speciesInfoCard: MaterialCardView
    private lateinit var btnZoomIn: ImageView
    private lateinit var fullScreenContainer: View
    private lateinit var fullScreenImage: ImageView
    private lateinit var btnZoomOut: ImageView

    // History screen views
    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var historyAdapter: HistoryAdapter

    // Managers
    private lateinit var viewModel: EcoLensViewModel
    private lateinit var navigationManager: NavigationManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var speciesInfoHandler: SpeciesInfoHandler

    private var imageUri: Uri? = null

    // Camera Activity Launcher
    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                val capturedUri = Uri.parse(uriString)
                imageUri = capturedUri

                Glide.with(this)
                    .load(capturedUri)
                    .centerCrop()
                    .into(imagePreview)
                btnZoomIn.visibility = View.VISIBLE
                viewModel.identifySpecies(capturedUri)
            }
        }
    }

    // Permission Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            permissionManager.showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initManagers()
        setupViewModel()
        setupBottomNavigation()
        setupHistoryScreen()
        setupFAB()
        setupObservers()
        setupZoomLogic()

        navigationManager.showHomeScreen()
    }

    private fun initViews() {
        // Containers
        homeContainer = findViewById(R.id.homeContainer)
        historyContainer = findViewById(R.id.historyContainer)
        myGardenContainer = findViewById(R.id.myGardenContainer)
        settingsContainer = findViewById(R.id.settingsContainer)

        // Home screen views
        fabSearch = findViewById(R.id.fabSearch)
        imagePreview = findViewById(R.id.imagePreview)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingCard = findViewById(R.id.loadingCard)
        errorCard = findViewById(R.id.errorCard)
        errorText = findViewById(R.id.errorText)
        speciesInfoCard = findViewById(R.id.speciesInfoCard)
        btnZoomIn = findViewById(R.id.btnZoomIn)
        fullScreenContainer = findViewById(R.id.fullScreenContainer)
        fullScreenImage = findViewById(R.id.fullScreenImage)
        btnZoomOut = findViewById(R.id.btnZoomOut)

        // History screen views
        rvHistory = historyContainer.findViewById(R.id.rvHistory)
        emptyStateContainer = historyContainer.findViewById(R.id.emptyStateContainer)
    }

    private fun initManagers() {
        navigationManager = NavigationManager(
            fabSearch, homeContainer, historyContainer,
            myGardenContainer, settingsContainer
        )

        permissionManager = PermissionManager(this, permissionLauncher)

        speciesInfoHandler = SpeciesInfoHandler(this, speciesInfoCard)
    }

    private fun setupZoomLogic() {
        btnZoomIn.setOnClickListener {
            imageUri?.let { uri ->
                fullScreenContainer.visibility = View.VISIBLE
                Glide.with(this)
                    .load(uri)
                    .into(fullScreenImage)
            }
        }
        btnZoomOut.setOnClickListener {
            fullScreenContainer.visibility = View.GONE
        }

        fullScreenContainer.setOnClickListener {
            fullScreenContainer.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (fullScreenContainer.visibility == View.VISIBLE) {
            fullScreenContainer.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EcoLensViewModel::class.java]
    }

    private fun setupHistoryScreen() {
        historyAdapter = HistoryAdapter(emptyList()) { entry ->
            openHistoryDetail(entry)
        }

        rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun openHistoryDetail(entry: HistoryEntry) {
        val detailFragment = HistoryDetailFragment()
        detailFragment.setData(entry)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom, R.anim.hold,
                R.anim.hold, R.anim.slide_out_bottom
            )
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("history_detail")
            .commit()
    }

    private fun setupBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottomNavigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> navigationManager.showHomeScreen()
                    R.id.nav_history -> navigationManager.showHistoryScreen()
                    R.id.nav_my_garden -> navigationManager.showMyGardenScreen()
                    R.id.nav_settings -> navigationManager.showSettingsScreen()
                    else -> return@setOnItemSelectedListener false
                }
                true
            }
    }

    private fun setupFAB() {
        findViewById<FloatingActionButton>(R.id.fabSearch).setOnClickListener {
            checkPermissionsAndOpenCamera()
        }
        findViewById<FloatingActionButton>(R.id.fabCamera).setOnClickListener {
            checkPermissionsAndOpenCamera()
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        if (permissionManager.hasPermissions()) {
            openCameraActivity()
        } else {
            permissionManager.requestPermissions()
        }
    }

    private fun openCameraActivity() {
        cameraActivityLauncher.launch(CameraActivity.newIntent(this))
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
    }

    private fun setupObservers() {
        // Observer cho UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (homeContainer.visibility == View.VISIBLE) {
                    updateUIState(state.isLoading, state.error)

                    state.speciesInfo?.let { info ->
                        speciesInfoHandler.displaySpeciesInfo(info, imageUri)
                        speciesInfoCard.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observer cho History List
        lifecycleScope.launch {
            viewModel.history.collect { historyList ->
                historyAdapter.updateList(historyList)
                updateHistoryVisibility(historyList.isNotEmpty())
            }
        }
    }

    private fun updateUIState(isLoading: Boolean, error: String?) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            speciesInfoCard.visibility = View.GONE
            errorCard.visibility = View.GONE
        }

        if (error != null) {
            errorText.text = error
            errorCard.visibility = View.VISIBLE
            speciesInfoCard.visibility = View.GONE
        } else {
            errorCard.visibility = View.GONE
        }
    }

    private fun updateHistoryVisibility(hasHistory: Boolean) {
        if (hasHistory) {
            rvHistory.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
        } else {
            rvHistory.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        }
    }
}