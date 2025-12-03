package com.nguyendevs.ecolens

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
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
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.fragments.HistoryDetailFragment
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.handlers.SpeciesInfoHandler
import com.nguyendevs.ecolens.managers.NavigationManager
import com.nguyendevs.ecolens.managers.PermissionManager
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.handlers.SettingsHandler
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.handlers.ImageZoomHandler
import com.nguyendevs.ecolens.handlers.LoadingAnimationHandler
import com.nguyendevs.ecolens.handlers.SearchBarHandler
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Containers
    private lateinit var homeContainer: View
    private lateinit var historyContainer: View
    private lateinit var myGardenContainer: View
    private lateinit var settingsContainer: View


    // Home screen views
    private lateinit var imagePreview: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var loadingCard: MaterialCardView
    private lateinit var errorCard: MaterialCardView
    private lateinit var errorText: TextView
    private lateinit var speciesInfoCard: MaterialCardView
    private lateinit var fabSpeak: FloatingActionButton
    private lateinit var fabMute: FloatingActionButton

    // History screen views
    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var historyAdapter: HistoryAdapter

    // Handlers
    private lateinit var searchBarHandler: SearchBarHandler
    private lateinit var imageZoomHandler: ImageZoomHandler
    private lateinit var loadingAnimationHandler: LoadingAnimationHandler
    private lateinit var settingsHandler: SettingsHandler

    // Managers
    private lateinit var speakerManager: SpeakerManager
    private lateinit var viewModel: EcoLensViewModel
    private lateinit var navigationManager: NavigationManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var speciesInfoHandler: SpeciesInfoHandler
    private lateinit var languageManager: LanguageManager

    private var imageUri: Uri? = null

    // Camera Activity Launcher
    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {

                if (searchBarHandler.isExpanded()) {
                    searchBarHandler.collapseSearchBar()
                }

                val capturedUri = Uri.parse(uriString)
                imageUri = capturedUri
                Glide.with(this)
                    .load(capturedUri)
                    .centerCrop()
                    .into(imagePreview)
                val currentLang = languageManager.getLanguage()

                imageZoomHandler.setImageUri(capturedUri)
                viewModel.identifySpecies(capturedUri, currentLang)
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

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initHandlers()
        initManagers()
        setupViewModel()
        setupBottomNavigation()
        setupHistoryScreen()
        setupFAB()
        setupObservers()

        navigationManager.showHomeScreen(false)
    }

    private fun initViews() {
        // Containers
        homeContainer = findViewById(R.id.homeContainer)
        historyContainer = findViewById(R.id.historyContainer)
        myGardenContainer = findViewById(R.id.myGardenContainer)
        settingsContainer = findViewById(R.id.settingsContainer)

        // Home screen views
        fabSpeak = findViewById(R.id.fabSpeak)
        fabMute = findViewById(R.id.fabMute)
        imagePreview = findViewById(R.id.imagePreview)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingCard = findViewById(R.id.loadingCard)
        errorCard = findViewById(R.id.errorCard)
        errorText = findViewById(R.id.errorText)
        speciesInfoCard = findViewById(R.id.speciesInfoCard)

        // History screen views
        rvHistory = historyContainer.findViewById(R.id.rvHistory)
        emptyStateContainer = historyContainer.findViewById(R.id.emptyStateContainer)
    }

    private fun initHandlers() {
        settingsHandler = SettingsHandler(this, languageManager, settingsContainer)
        settingsHandler.setup()

        // Search Bar Handler
        searchBarHandler = SearchBarHandler(
            this,
            findViewById(R.id.searchBarContainer),
            findViewById(R.id.textInputLayoutSearch),
            findViewById(R.id.etSearchQuery),
            findViewById(R.id.btnSearchAction)
        )
        searchBarHandler.setup()

        // Image Zoom Handler
        imageZoomHandler = ImageZoomHandler(
            findViewById(R.id.btnZoomIn),
            findViewById(R.id.btnZoomOut),
            findViewById(R.id.fullScreenContainer),
            findViewById(R.id.fullScreenImage)
        )
        imageZoomHandler.setup()

        // Loading Animation Handler
        loadingAnimationHandler = LoadingAnimationHandler(
            loadingCard.findViewById(R.id.tvLoading),
            lifecycleScope
        )
    }

    private fun initManagers() {
        navigationManager = NavigationManager(
            findViewById(R.id.searchBarContainer),
            fabSpeak,
            homeContainer,
            historyContainer,
            myGardenContainer,
            settingsContainer

        )

        permissionManager = PermissionManager(this, permissionLauncher)

        speciesInfoHandler = SpeciesInfoHandler(this, speciesInfoCard) { copiedText ->
            searchBarHandler.expandSearchBar(copiedText)
        }

        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = {
            runOnUiThread {
                toggleSpeakerUI(isSpeaking = false)
            }
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
                    R.id.nav_home -> {
                        val state = viewModel.uiState.value
                        val hasInfo = state.speciesInfo != null
                                && !state.isLoading
                                && state.error == null
                        navigationManager.showHomeScreen(hasInfo)

                        if (hasInfo) {
                            toggleSpeakerUI(speakerManager.isSpeaking())
                        } else {
                            fabMute.visibility = View.GONE
                        }
                    }
                    R.id.nav_history -> {
                        navigationManager.showHistoryScreen()
                        fabMute.visibility = View.GONE
                    }
                    R.id.nav_my_garden -> {
                        navigationManager.showMyGardenScreen()
                        fabMute.visibility = View.GONE
                    }
                    R.id.nav_settings -> {
                        navigationManager.showSettingsScreen()
                        fabMute.visibility = View.GONE
                    }
                    else -> return@setOnItemSelectedListener false
                }
                true
            }
    }

    private fun toggleSpeakerUI(isSpeaking: Boolean) {
        if (isSpeaking) {
            fabSpeak.visibility = View.GONE
            fabMute.visibility = View.VISIBLE
        } else {
            fabSpeak.visibility = View.VISIBLE
            fabMute.visibility = View.GONE
        }
    }

    private fun setupFAB() {
        findViewById<FloatingActionButton>(R.id.fabCamera).setOnClickListener {
            checkPermissionsAndOpenCamera()
        }
        fabSpeak.setOnClickListener {
            viewModel.uiState.value.speciesInfo?.let { info ->
                val textToRead = TextToSpeechGenerator.generateSpeechText(this, info)
                if (textToRead.isNotEmpty()) {
                    speakerManager.setLanguage(languageManager.getLanguage())
                    speakerManager.speak(textToRead)
                    toggleSpeakerUI(isSpeaking = true)
                }
            }
        }
        fabMute.setOnClickListener {
            speakerManager.pause()
            toggleSpeakerUI(isSpeaking = false)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        KeyboardUtils.handleTouchEvent(this, event)
        return super.dispatchTouchEvent(event)
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
            loadingAnimationHandler.start()
        } else {
            loadingAnimationHandler.stop()
        }

        if (isLoading) {
            speciesInfoCard.visibility = View.GONE
            errorCard.visibility = View.GONE
        }

        if (viewModel.uiState.value.speciesInfo != null && !isLoading && error == null) {
            if (fabMute.visibility != View.VISIBLE) {
                fabSpeak.visibility = View.VISIBLE
            }
        } else {
            fabSpeak.visibility = View.GONE
            fabMute.visibility = View.GONE
            speakerManager.pause()
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

    override fun onResume() {
        super.onResume()
        settingsHandler.updateLanguageDisplay()
    }

    override fun onDestroy() {
        speakerManager.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragmentContainer)

        // Nếu fragment container đang hiển thị
        if (fragmentContainer.visibility == View.VISIBLE) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                fragmentContainer.visibility = View.GONE
                return
            }
        }
        // Xử lý back cho zoom image
        if (imageZoomHandler.isFullScreenVisible()) {
            imageZoomHandler.hideFullScreen()
        } else {
            super.onBackPressed()
        }
    }
}