package com.nguyendevs.ecolens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.fragments.chat.ChatHistoryFragment
import com.nguyendevs.ecolens.fragments.history.HistoryFragment
import com.nguyendevs.ecolens.handlers.*
import com.nguyendevs.ecolens.handlers.animations.HistoryDetailAnimationHandler
import com.nguyendevs.ecolens.handlers.animations.LoadingAnimationHandler
import com.nguyendevs.ecolens.handlers.home.ImageZoomHandler
import com.nguyendevs.ecolens.handlers.home.SearchBarHandler
import com.nguyendevs.ecolens.handlers.main.HomeScreenHandler
import com.nguyendevs.ecolens.handlers.main.NavigationHandler
import com.nguyendevs.ecolens.handlers.setting.SettingsHandler
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.managers.main.PermissionManager
import com.nguyendevs.ecolens.managers.main.SpeakerManager
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import com.nguyendevs.ecolens.models.LoadingStage
import com.nguyendevs.ecolens.models.SpeciesInfo
import com.nguyendevs.ecolens.utils.FabAnimationHelper
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import java.lang.ref.WeakReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

/** Activity chính của ứng dụng EcoLens. Quản lý navigation, nhận diện loài, và UI chính. */
class MainActivity : AppCompatActivity() {
    private val FAB_ANIM_DURATION = 180L
    private val FAB_SCALE_DOWN = 0.82f
    private val FAB_SCALE_NORMAL = 1f
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: EcoLensViewModel

    private lateinit var imageZoomHandler: ImageZoomHandler
    private lateinit var languageManager: LanguageManager
    private lateinit var loadingAnimationHandler: LoadingAnimationHandler
    private lateinit var permissionManager: PermissionManager
    private lateinit var searchBarHandler: SearchBarHandler
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var speakerManager: SpeakerManager
    private lateinit var speciesInfoHandler: SpeciesInfoHandler
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var homeScreenHandler: HomeScreenHandler
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var animationHandler: HistoryDetailAnimationHandler
    private val userRepository = UserRepository()

    private val historyFragment: HistoryFragment by lazy {
        supportFragmentManager.findFragmentByTag("HISTORY") as? HistoryFragment ?: HistoryFragment()
    }
    private val chatHistoryFragment: ChatHistoryFragment by lazy {
        supportFragmentManager.findFragmentByTag("CHAT_HISTORY") as? ChatHistoryFragment ?: ChatHistoryFragment()
    }

    private val homeViews by lazy { HomeViews(binding.homeContainer.root) }

    private class HomeViews(root: View) {
        val heroCard: View? = root.findViewById(R.id.heroCard)
        val imgHeroFull: android.widget.ImageView? = root.findViewById(R.id.imgHeroFull)
        val sectionQuickExplore: View? = root.findViewById(R.id.sectionQuickExplore)
        val sectionRecent: View? = root.findViewById(R.id.sectionRecent)
        val tvGreeting: TextView? = root.findViewById(R.id.tvGreeting)
        val tvAppTitle: TextView? = root.findViewById(R.id.tvAppTitle)
        val progressBarHero: View? = root.findViewById(R.id.progressBarHero)
        val tvLoadingText: TextView? = root.findViewById(R.id.tvLoadingText)
        val speciesInfoCard: View? = root.findViewById(R.id.speciesInfoCard)
        val errorCard: View? = root.findViewById(R.id.errorCard)
        val loadingCard: View? = root.findViewById(R.id.loadingCard)
        val tvHeroBadge: View? = root.findViewById(R.id.tvHeroBadge)
        val tvHeroTitle: View? = root.findViewById(R.id.tvHeroTitle)
        val tvHeroSubtitle: View? = root.findViewById(R.id.tvHeroSubtitle)
        val btnStartNow: View? = root.findViewById(R.id.btnStartNow)
        val imgHeroDecor: View? = root.findViewById(R.id.imgHeroDecor)
        val zoomControls: View? = root.findViewById(R.id.zoomControls)
        val errorText: TextView? = root.findViewById(R.id.errorText)
    }
    private var imageUri: Uri? = null
    private var isExpandedState = false
    private var stopLoadingJob: Job? = null
    private var isSpeaking = false

    companion object {
        private const val PREF_NAME = "EcoLensPrefs"
        private const val TRANSITION_ANIMATION_DURATION = 400L
        private const val PRELOAD_DELAY_MS = 500L

        private var transitionBitmapRef: WeakReference<Bitmap>? = null

        var transitionBitmap: Bitmap?
            get() = transitionBitmapRef?.get()
            set(value) {
                transitionBitmapRef = value?.let { WeakReference(it) }
            }
    }

    /** Launcher cho CameraActivity */
    private val cameraActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
                    if (uriString != null) {
                        val lat = result.data?.getDoubleExtra(CameraActivity.KEY_LAT, Double.MIN_VALUE)
                        val lng = result.data?.getDoubleExtra(CameraActivity.KEY_LNG, Double.MIN_VALUE)
                        val hasValidCoords = lat != null && lat != Double.MIN_VALUE &&
                                lng != null && lng != Double.MIN_VALUE
                        if (hasValidCoords) {
                            handleCapturedImage(uriString.toUri(), lat!!, lng!!)
                        } else {
                            handleCapturedImage(uriString.toUri())
                        }
                    }
                }
            }

    /** Launcher cho yêu cầu quyền */
    private val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                if (permissions.values.all { it }) {
                    cameraActivityLauncher.launch(CameraActivity.newIntent(this))
                    overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
                } else {
                    permissionManager.showPermissionDeniedDialog()
                }
            }

    /** Launcher cho Google Re-Auth */
    private val googleReAuthLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let { idToken ->
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            userRepository.reauthenticateUser(credential) { success ->
                                if (success) {
                                    settingsHandler.onGoogleReAuthSuccess()
                                } else {
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.error_reauth_failed),
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                }
                            }
                        }
                    } catch (e: ApiException) {
                        Toast.makeText(
                                        this,
                                        "Google re-auth failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            }

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadThemePreference()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleTransitionAnimation()

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setupViewModel()

        viewModel.currentImageUri?.let { uri -> this.imageUri = uri }

        initHandlers()
        initManagers()

        homeScreenHandler.setup()
        navigationHandler.setup()

        setupFAB()
        setupObservers()
        setupBackNavigation()

        val navigateToSettings = intent.getBooleanExtra("navigate_to_settings", false)
        val lastNavItem = navigationHandler.restoreLastTab(navigateToSettings)

        binding.bottomNavigation.selectedItemId = lastNavItem
        navigationHandler.updateNavigationState(
                lastNavItem,
                uiStateChecker = {
                    val state = viewModel.uiState.value
                    val isComplete = state.loadingStage == LoadingStage.COMPLETE
                    val hasInfo =
                            state.speciesInfo != null && !state.isLoading && state.error == null
                    Triple(state.loadingStage, speakerManager.isSpeaking(), hasInfo)
                }
        )

        preloadFragments()
        syncUserData()
    }

    /** Đồng bộ dữ liệu người dùng từ Firebase về SharedPreferences */
    private fun syncUserData() {
        if (userRepository.isUserLoggedIn()) {
            lifecycleScope.launch {
                val user = userRepository.getCurrentUserDetails()
                if (user != null) {
                    val appSettings = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    appSettings
                            .edit()
                            .putBoolean("iucn_mode", user.iucnMode)
                            .putBoolean("taxo_mode", user.taxoMode)
                            .apply()

                    settingsHandler.refreshSettingsState()

                    val currentLang = languageManager.getLanguage()
                    if (user.language != currentLang) {
                        languageManager.setLanguage(user.language)

                        val intent = Intent(this@MainActivity, MainActivity::class.java)
                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                        startActivity(intent)
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            }
        }
    }

    private fun preloadFragments() {
        lifecycleScope.launch {
            delay(PRELOAD_DELAY_MS)
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                val transaction = supportFragmentManager.beginTransaction()
                if (!historyFragment.isAdded) {
                    transaction.add(R.id.historyContainer, historyFragment, "HISTORY")
                }
                if (!chatHistoryFragment.isAdded) {
                    transaction.add(
                            R.id.myGardenContainer,
                            chatHistoryFragment,
                            "CHAT_HISTORY"
                    )
                }
                transaction.commitAllowingStateLoss()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        languageManager.updateBaseContext(this)
    }

    private fun handleTransitionAnimation() {
        val bitmap = transitionBitmap ?: return

        val rootView = window.decorView as ViewGroup
        val overlay =
                ImageView(this).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_XY
                    elevation = 100f
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                }
        rootView.addView(overlay)

        overlay.animate()
                .alpha(0f)
                .setDuration(TRANSITION_ANIMATION_DURATION)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    rootView.removeView(overlay)
                    transitionBitmap?.let { bmp ->
                        if (!bmp.isRecycled) {
                            bmp.recycle()
                        }
                    }
                    transitionBitmap = null
                }
                .start()
    }

    /** Tải cài đặt theme từ SharedPreferences */
    private fun loadThemePreference() {
        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)

        val nightMode =
                if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Khởi tạo ViewModel */
    private fun setupViewModel() {
        viewModel =
                ViewModelProvider(
                        this,
                        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[EcoLensViewModel::class.java]
    }

    /** Khởi tạo các handler xử lý UI */
    private fun initHandlers() {
        settingsHandler =
                SettingsHandler(
                        this,
                        languageManager,
                        binding.settingsContainer,
                        onUsernameChanged = { homeScreenHandler.setupGreeting() }
                )

        settingsHandler.setGoogleReAuthRequest {
            val gso =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)

            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleReAuthLauncher.launch(signInIntent)
            }
        }

        val homeRoot = binding.homeContainer.root

        searchBarHandler =
                SearchBarHandler(
                        this,
                        binding.searchBarContainer,
                        binding.textInputLayoutSearch,
                        binding.etSearchQuery,
                        binding.btnSearchAction
                ).apply {
                    val homeRoot = binding.homeContainer.root
                    setViewsToHide(listOf(
                        homeRoot.findViewById(R.id.tvAppTitle),
                        homeRoot.findViewById(R.id.tvGreeting)
                    ))
                }

        val btnZoomIn = homeRoot.findViewById<ImageView>(R.id.btnZoomIn)

        imageZoomHandler =
                ImageZoomHandler(
                        btnZoomIn,
                        binding.btnZoomOut,
                        binding.fullScreenContainer,
                        binding.fullScreenImage
                )

        loadingAnimationHandler =
                LoadingAnimationHandler(homeRoot.findViewById(R.id.tvLoadingText), lifecycleScope)

        speciesInfoHandler =
                SpeciesInfoHandler(
                        this,
                        binding.homeContainer.speciesInfoCard,
                        { text ->
                            searchBarHandler.expandSearchBar(text)
                        },
                        { viewModel.retryIdentification() }
                )

        homeScreenHandler =
                HomeScreenHandler(
                        this,
                        binding,
                        { entry -> navigateToHistoryDetail(entry) },
                        { imageUrl -> handleCapturedImage(imageUrl.toUri()) }
                )

        navigationHandler =
                NavigationHandler(
                        this,
                        binding,
                        sharedPreferences,
                        historyFragment,
                        chatHistoryFragment
                ) { itemId ->
                    if (speakerManager.isSpeaking()) {
                        speakerManager.pause()
                        updateFabUI(false)
                    }

                    if (itemId == R.id.nav_home) {
                        val state = viewModel.uiState.value
                        val isComplete = state.loadingStage == LoadingStage.COMPLETE
                        val hasInfo =
                                state.speciesInfo != null && !state.isLoading && state.error == null

                        if (isComplete && hasInfo) {
                            binding.fabSpeak.isVisible = true
                        }
                    } else {
                        binding.fabSpeak.isVisible = false
                    }
                }

        animationHandler = HistoryDetailAnimationHandler(this)
    }

    /** Khởi tạo các manager */
    private fun initManagers() {
        permissionManager = PermissionManager(this, permissionLauncher)
        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = { runOnUiThread { updateFabUI(false) } }

        supportFragmentManager.addOnBackStackChangedListener {
            val count = supportFragmentManager.backStackEntryCount
            if (count > 0) {
                binding.fragmentContainer.visibility = View.VISIBLE
            } else {
                binding.fragmentContainer.postDelayed(
                        {
                            if (supportFragmentManager.backStackEntryCount == 0) {
                                binding.fragmentContainer.visibility = View.GONE
                            }
                        },
                        400
                )
            }
        }
    }

    /**
     * Chuyển đổi trạng thái hiển thị của Home Screen
     * @param showResults: true -> Hiển thị kết quả/loading, ẩn Home content
     * ```
     *                 false -> Hiển thị Home content, ẩn kết quả
     * ```
     */
    private fun toggleHomeState(showResults: Boolean) {
        val homeRoot = binding.homeContainer.root as ViewGroup

        if (!showResults) {
            androidx.transition.TransitionManager.beginDelayedTransition(homeRoot)
        }

        if (showResults) {
            homeViews.sectionQuickExplore?.visibility = View.GONE
            homeViews.sectionRecent?.visibility = View.GONE

            homeViews.heroCard?.visibility = View.VISIBLE
            homeViews.imgHeroFull?.visibility = View.VISIBLE
            homeViews.zoomControls?.visibility = View.VISIBLE

            homeViews.tvHeroBadge?.visibility = View.GONE
            homeViews.tvHeroTitle?.visibility = View.GONE
            homeViews.tvHeroSubtitle?.visibility = View.GONE
            homeViews.btnStartNow?.visibility = View.GONE
            homeViews.imgHeroDecor?.visibility = View.GONE

            homeViews.loadingCard?.visibility = View.GONE

            this.imageUri?.let { uri ->
                val path = uri.path
                val loadModel =
                        if (path != null) {
                            java.io.File(path).takeIf { it.exists() } ?: uri
                        } else uri

                homeViews.imgHeroFull?.let {
                    com.bumptech.glide.Glide.with(this)
                            .load(loadModel)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerCrop()
                            .into(it)
                }
                
                imageZoomHandler.setImageUri(uri)
            }
        } else {
            homeViews.sectionQuickExplore?.visibility = View.VISIBLE
            homeViews.sectionRecent?.visibility = View.VISIBLE
            homeViews.tvGreeting?.visibility = View.VISIBLE
            homeViews.tvAppTitle?.visibility = View.VISIBLE

            homeViews.heroCard?.visibility = View.VISIBLE
            homeViews.imgHeroFull?.visibility = View.GONE
            homeViews.zoomControls?.visibility = View.GONE

            homeViews.tvHeroBadge?.visibility = View.VISIBLE
            homeViews.tvHeroTitle?.visibility = View.VISIBLE
            homeViews.tvHeroSubtitle?.visibility = View.VISIBLE
            homeViews.btnStartNow?.visibility = View.VISIBLE
            homeViews.imgHeroDecor?.visibility = View.VISIBLE

            homeViews.progressBarHero?.visibility = View.GONE
            homeViews.tvLoadingText?.visibility = View.GONE
            homeViews.speciesInfoCard?.visibility = View.GONE
            homeViews.errorCard?.visibility = View.GONE
            homeViews.loadingCard?.visibility = View.GONE

            binding.fabCamera.isClickable = true
            binding.fabCamera.alpha = 1.0f
        }
    }

    /** Xử lý ảnh đã chụp từ camera hoặc upload từ gallery */
    private fun handleCapturedImage(
            uri: Uri,
            lat: Double = 16.0544,
            lng: Double = 108.2022
    ) {
        binding.fabCamera.isClickable = false
        binding.fabCamera.alpha = 0.5f

        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
            updateFabUI(false)
        }

        if (searchBarHandler.isExpanded()) searchBarHandler.collapseSearchBar()

        navigationHandler.navigateTo(R.id.nav_home)

        viewModel.currentImageUri = uri
        this.imageUri = uri
        speciesInfoHandler.setImageUri(uri)

        toggleHomeState(showResults = true)

        viewModel.identifySpecies(uri, languageManager.getLanguage(), lat = lat, lng = lng)
    }

    /** Thiết lập các Floating Action Button và Camera setup */
    private fun setupFAB() {
        binding.fabCamera.setOnClickListener {
            binding.fabCamera.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            FabAnimationHelper.animateClick(binding.fabCamera) {
                if (speakerManager.isSpeaking()) {
                    speakerManager.pause()
                    updateFabUI(false)
                }

                if (permissionManager.hasPermissions()) {
                    cameraActivityLauncher.launch(CameraActivity.newIntent(this))
                    overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
                } else {
                    permissionManager.requestPermissions()
                }
            }
        }

        binding.fabSpeak.setOnClickListener {
            viewModel.uiState.value.speciesInfo?.let { info ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if (isSpeaking) {
                        speakerManager.pause()
                        withContext(Dispatchers.Main) { updateFabUI(false) }
                    } else {
                        val text = TextToSpeechGenerator.generateSpeechText(this@MainActivity, info)
                        if (text.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                speakerManager.setLanguage(languageManager.getLanguage())
                                speakerManager.speak(text)
                                updateFabUI(true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateFabUI(speaking: Boolean) {
        isSpeaking = speaking
        animationHandler.animateFabState(binding.fabSpeak, speaking)
    }

    /** Thiết lập observers cho UI state */
    private fun setupObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> updateHomeUI(state) }
            }
        }
    }

    /** Cập nhật UI màn hình Home dựa trên state */
    private suspend fun updateHomeUI(state: com.nguyendevs.ecolens.models.EcoLensUiState) {
        val isLoading = state.isLoading
        val error = state.error
        val loadingStage = state.loadingStage

        val isPhase2 =
                loadingStage == LoadingStage.DESCRIPTION ||
                        loadingStage == LoadingStage.CHARACTERISTICS ||
                        loadingStage == LoadingStage.DISTRIBUTION ||
                        loadingStage == LoadingStage.HABITAT ||
                        loadingStage == LoadingStage.CONSERVATION

        binding.fabCamera.isClickable = !isLoading
        binding.fabCamera.alpha = if (isLoading) 0.5f else 1.0f

        val homeRoot = binding.homeContainer.root
        val progressBarHero = homeViews.progressBarHero
        val tvLoadingText = homeViews.tvLoadingText
        val loadingCard = homeViews.loadingCard

        loadingCard?.isVisible = false

        if (isLoading || state.speciesInfo != null || error != null) {
            toggleHomeState(showResults = true)
        } else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null && error == null
        ) {
            toggleHomeState(showResults = false)
        }

        if (isLoading) {
            progressBarHero?.isVisible = true
            tvLoadingText?.isVisible = true
            loadingAnimationHandler.setText(R.string.analyzing_text)
            stopLoadingJob?.cancel()

            when (loadingStage) {
                LoadingStage.SCIENTIFIC_NAME, LoadingStage.COMMON_NAME ->
                        loadingAnimationHandler.setText(R.string.loading_taxon)
                LoadingStage.TAXONOMY -> loadingAnimationHandler.setText(R.string.loading_detail)
                LoadingStage.DESCRIPTION,
                LoadingStage.CHARACTERISTICS,
                LoadingStage.DISTRIBUTION,
                LoadingStage.HABITAT,
                LoadingStage.CONSERVATION ->
                        loadingAnimationHandler.setText(R.string.loading_conservation)
                else -> loadingAnimationHandler.setText(R.string.analyzing_text)
            }
            loadingAnimationHandler.start()
        } else {
            loadingAnimationHandler.setText(R.string.loading_done)
            progressBarHero?.isVisible = false

            if (isPhase2) {
                stopLoadingJob?.cancel()
                loadingAnimationHandler.stop()
                tvLoadingText?.isVisible = false
            } else {
                stopLoadingJob?.cancel()
                stopLoadingJob =
                        lifecycleScope.launch {
                            delay(500)
                            loadingAnimationHandler.stop()
                            tvLoadingText?.isVisible = false
                        }
            }
        }

        if (error != null) {
            homeViews.errorText?.text = error
            val errorCard = homeViews.errorCard
            if (errorCard != null) errorCard.isVisible = true

            progressBarHero?.isVisible = false
            tvLoadingText?.isVisible = false

            binding.homeContainer.speciesInfoCard.root.isVisible = false
            binding.fabSpeak.isVisible = false

            binding.fabCamera.isClickable = true
            binding.fabCamera.alpha = 1.0f
        } else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = false
            homeViews.errorCard?.isVisible = false
            binding.fabSpeak.isVisible = false
            speciesInfoHandler.displaySpeciesInfo(
                    SpeciesInfo(scientificName = "", commonName = ""),
                    LoadingStage.NONE
            )
        } else if (state.speciesInfo != null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = true
            val errorCard = homeViews.errorCard
            if (errorCard != null) errorCard.isVisible = false

            speciesInfoHandler.displaySpeciesInfo(
                    state.speciesInfo,
                    loadingStage,
                    state.isTaxonomyTranslating,
                    state.images,
                    state.historyId,
                    state.isFavorite,
                    onFavoriteToggle = { id, favorite ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val entry = viewModel.getHistoryBySortOption(com.nguyendevs.ecolens.models.history.HistorySortOption.NEWEST_FIRST)
                                .firstOrNull()?.find { it.id == id }
                            
                            if (entry != null) {
                                viewModel.toggleFavorite(entry)
                            }
                        }
                    }
            )

            if (navigationHandler.isHomeTab()) {
                if (loadingStage == LoadingStage.COMPLETE) {
                    if (binding.fabSpeak.visibility != View.VISIBLE) {
                        binding.fabSpeak.isVisible = true
                        animationHandler.showFab(binding.fabSpeak)
                    }
                } else {
                    binding.fabSpeak.isVisible = false
                }
            } else {
                binding.fabSpeak.isVisible = false
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        KeyboardUtils.handleTouchEvent(this, event)
        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        settingsHandler.updateLanguageDisplay()
    }

    override fun onDestroy() {
        speakerManager.onSpeechFinished = null
        speakerManager.shutdown()
        speciesInfoHandler.onDestroy()
        super.onDestroy()
    }
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (speakerManager.isSpeaking()) {
                            speakerManager.pause()
                            updateFabUI(false)
                            return
                        }

                        if (imageZoomHandler.isFullScreenVisible()) {
                            imageZoomHandler.hideFullScreen()
                            return
                        }

                        if (supportFragmentManager.backStackEntryCount > 0) {
                            supportFragmentManager.popBackStack()
                            return
                        }

                        if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
                            binding.bottomNavigation.selectedItemId = R.id.nav_home
                        } else {
                            val state = viewModel.uiState.value
                            if (state.isLoading) return

                            val hasData = state.speciesInfo != null || state.error != null
                            if (hasData) {
                                viewModel.resetState()
                                binding.homeContainer.homeScrollView.postDelayed(
                                        { smoothScrollToTop() },
                                        900
                                )
                            } else {
                                isEnabled = false
                                onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    }
                }
        )
    }

    private fun smoothScrollToTop() {
        val scrollView = binding.homeContainer.homeScrollView
        val animator =
                android.animation.ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.scrollY, 0)
        animator.duration = 600
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.start()
    }

    /** Chuyển hướng tới màn hình chi tiết lịch sử. */
    private fun navigateToHistoryDetail(entry: com.nguyendevs.ecolens.models.history.HistoryEntry) {
        val jsonEntry = com.google.gson.Gson().toJson(entry)
        val fragment =
                com.nguyendevs.ecolens.fragments.history.HistoryDetailFragment().apply {
                    arguments = Bundle().apply { putString("HISTORY_ENTRY_JSON", jsonEntry) }
                }

        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.hold,
                        R.anim.hold,
                        R.anim.slide_out_bottom
                )
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        binding.fragmentContainer.visibility = View.VISIBLE
    }
}
