package com.nguyendevs.ecolens

import android.content.Context
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
import android.widget.ProgressBar
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.adapters.RecentHistoryAdapter
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.fragments.chat.ChatHistoryFragment
import com.nguyendevs.ecolens.fragments.history.HistoryFragment
import com.nguyendevs.ecolens.handlers.*
import com.nguyendevs.ecolens.handlers.animation.LoadingAnimationHandler
import com.nguyendevs.ecolens.handlers.interaction.ImageZoomHandler
import com.nguyendevs.ecolens.handlers.interaction.SearchBarHandler
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import java.lang.ref.WeakReference
import java.util.Calendar
import kotlinx.coroutines.*

/** Activity chính của ứng dụng EcoLens. Quản lý navigation, nhận diện loài, và UI chính. */
class MainActivity : AppCompatActivity() {

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

    private val historyFragment = HistoryFragment()
    private val chatHistoryFragment = ChatHistoryFragment()
    private var imageUri: Uri? = null
    private var isExpandedState = false
    private var stopLoadingJob: Job? = null

    // Recent History on Home Screen
    private lateinit var recentHistoryAdapter: RecentHistoryAdapter
    private val historyRepository by lazy {
        HistoryRepository(
                HistoryDatabase.getDatabase(applicationContext).historyDao(),
                applicationContext
        )
    }

    companion object {
        private const val PREF_NAME = "EcoLensPrefs"
        private const val KEY_LAST_NAV_ITEM = "last_nav_item"

        // UI Constants
        private const val IMAGE_PREVIEW_HEIGHT_DP = 290
        private const val TRANSITION_ANIMATION_DURATION = 400L
        private const val PRELOAD_DELAY_MS = 500L
        private const val LOADING_STOP_DELAY_MS = 500L

        /** Bitmap dùng cho transition animation. Sử dụng WeakReference tránh memory leak. */
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
                        handleCapturedImage(uriString.toUri())
                    }
                }
            }

    /** Launcher cho yêu cầu quyền */
    private val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                if (!permissions.values.all { it }) {
                    permissionManager.showPermissionDeniedDialog()
                }
            }

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadThemePreference()
        super.onCreate(savedInstanceState)

        // Firebase persistence đã được khởi tạo trong EcoLensApplication

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleTransitionAnimation()

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setupViewModel()

        viewModel.currentImageUri?.let { uri ->
            this.imageUri = uri
            // binding.root.post { restoreExpandedState(uri) }
        }

        initHandlers()
        initManagers()
        setupBottomNavigation()
        setupFAB()
        setupObservers()
        setupBackNavigation()
        setupHomeScreen()

        val navigateToSettings = intent.getBooleanExtra("navigate_to_settings", false)
        val lastNavItem =
                if (navigateToSettings) {
                    R.id.nav_settings
                } else {
                    sharedPreferences.getInt(KEY_LAST_NAV_ITEM, R.id.nav_home)
                }

        binding.bottomNavigation.selectedItemId = lastNavItem
        binding.root.post { updateNavigationState(lastNavItem) }

        preloadFragments()
    }

    /**
     * Preload các fragment để cải thiện hiệu suất Sử dụng IdleHandler để load khi UI thread rảnh
     */
    private fun preloadFragments() {
        Looper.myQueue().addIdleHandler {
            if (!isDestroyed && !isFinishing) {
                lifecycleScope.launch {
                    delay(PRELOAD_DELAY_MS)
                    if (!isDestroyed) {
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
            false // Remove handler sau khi thực thi
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /** Xử lý transition animation từ splash screen Sử dụng WeakReference để tránh memory leak */
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
                    // Recycle bitmap nếu còn available
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
        settingsHandler = SettingsHandler(this, languageManager, binding.settingsContainer)

        val homeRoot = binding.homeContainer.root

        searchBarHandler =
                SearchBarHandler(
                        this,
                        binding.searchBarContainer,
                        binding.textInputLayoutSearch,
                        binding.etSearchQuery,
                        binding.btnSearchAction
                )

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
                            Toast.makeText(
                                            this,
                                            getString(R.string.copy_scientific_name) + ": " + text,
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        },
                        { viewModel.retryIdentification() }
                )
    }

    /** Khởi tạo các manager */
    private fun initManagers() {
        permissionManager = PermissionManager(this, permissionLauncher)
        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = { runOnUiThread { toggleSpeakerUI(false) } }

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

        // Chỉ chạy animation khi quay lại màn hình Home (showResults = false).
        // Khi hiển thị kết quả (showResults = true), ẩn ngay lập tức để tránh delay.
        if (!showResults) {
            androidx.transition.TransitionManager.beginDelayedTransition(homeRoot)
        }

        // Home Screen Elements
        val heroCard = homeRoot.findViewById<View>(R.id.heroCard)
        val imgHeroFull = homeRoot.findViewById<android.widget.ImageView>(R.id.imgHeroFull)
        val sectionQuickExplore = homeRoot.findViewById<View>(R.id.sectionQuickExplore)
        val sectionRecent = homeRoot.findViewById<View>(R.id.sectionRecent)
        val tvGreeting = homeRoot.findViewById<TextView>(R.id.tvGreeting)
        val tvAppTitle = homeRoot.findViewById<TextView>(R.id.tvAppTitle)

        // Result/Loading Elements
        val progressBarHero = homeRoot.findViewById<View>(R.id.progressBarHero)
        val tvLoadingText = homeRoot.findViewById<View>(R.id.tvLoadingText)
        val speciesInfoCard = homeRoot.findViewById<View>(R.id.speciesInfoCard)
        val errorCard = homeRoot.findViewById<View>(R.id.errorCard)
        val loadingCard = homeRoot.findViewById<View>(R.id.loadingCard) 

        // Hero Content Elements (to hide when showing result)
        val tvHeroBadge = homeRoot.findViewById<View>(R.id.tvHeroBadge)
        val tvHeroTitle = homeRoot.findViewById<View>(R.id.tvHeroTitle)
        val tvHeroSubtitle = homeRoot.findViewById<View>(R.id.tvHeroSubtitle)
        val btnStartNow = homeRoot.findViewById<View>(R.id.btnStartNow)
        val imgHeroDecor = homeRoot.findViewById<View>(R.id.imgHeroDecor)
        val zoomControls = homeRoot.findViewById<View>(R.id.zoomControls)

        if (showResults) {
            // HIDE Standard Home Sections
            sectionQuickExplore?.visibility = View.GONE
            sectionRecent?.visibility = View.GONE

            // MODIFY Hero Card
            heroCard?.visibility = View.VISIBLE
            imgHeroFull?.visibility = View.VISIBLE
            zoomControls?.visibility = View.VISIBLE

            // Hide Hero Default Content
            tvHeroBadge?.visibility = View.GONE
            tvHeroTitle?.visibility = View.GONE
            tvHeroSubtitle?.visibility = View.GONE
            btnStartNow?.visibility = View.GONE
            imgHeroDecor?.visibility = View.GONE
            
            // Hide legacy loading card
            loadingCard?.visibility = View.GONE

            // Load Image if URI exists
            this.imageUri?.let { uri ->
                val path = uri.path
                val loadModel =
                        if (path != null) {
                            java.io.File(path).takeIf { it.exists() } ?: uri
                        } else uri

                // Use centerCrop to fill the card
                com.bumptech.glide.Glide.with(this)
                    .load(loadModel)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgHeroFull)

                // Update Zoom Handler
                imageZoomHandler.setImageUri(uri)
            }
        } else {
            // RESTORE Home Content
            sectionQuickExplore?.visibility = View.VISIBLE
            sectionRecent?.visibility = View.VISIBLE
            tvGreeting?.visibility = View.VISIBLE
            tvAppTitle?.visibility = View.VISIBLE

            // RESTORE Hero Card
            heroCard?.visibility = View.VISIBLE
            imgHeroFull?.visibility = View.GONE
            zoomControls?.visibility = View.GONE

            tvHeroBadge?.visibility = View.VISIBLE
            tvHeroTitle?.visibility = View.VISIBLE
            tvHeroSubtitle?.visibility = View.VISIBLE
            btnStartNow?.visibility = View.VISIBLE
            imgHeroDecor?.visibility = View.VISIBLE

            // Hide result stuff
            progressBarHero?.visibility = View.GONE
            tvLoadingText?.visibility = View.GONE
            speciesInfoCard?.visibility = View.GONE
            errorCard?.visibility = View.GONE
            loadingCard?.visibility = View.GONE

            // Reset FAB state
            binding.fabCamera.isClickable = true
            binding.fabCamera.alpha = 1.0f
        }
    }

    /** Xử lý ảnh đã chụp từ camera */
    private fun handleCapturedImage(uri: Uri) {
        // Prevent double clicks
        binding.fabCamera.isClickable = false
        binding.fabCamera.alpha = 0.5f

        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }

        if (searchBarHandler.isExpanded()) searchBarHandler.collapseSearchBar()

        binding.bottomNavigation.selectedItemId = R.id.nav_home

        viewModel.currentImageUri = uri
        this.imageUri = uri

        // SWITCH STATE: Hide Home content, show loading
        toggleHomeState(showResults = true)

        // Trigger identification
        viewModel.identifySpecies(uri, languageManager.getLanguage())
    }

    /** Thiết lập bottom navigation */
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.bottomNavigation.performHapticFeedback(
                    android.view.HapticFeedbackConstants.CONFIRM
            )
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(
                        null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }

            sharedPreferences.edit().putInt(KEY_LAST_NAV_ITEM, item.itemId).apply()

            updateNavigationState(item.itemId)
            true
        }
        
        binding.fabCamera.setOnClickListener {
            binding.fabCamera.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            if (speakerManager.isSpeaking()) {
                speakerManager.pause()
                toggleSpeakerUI(false)
            }

            if (permissionManager.hasPermissions()) {
                cameraActivityLauncher.launch(CameraActivity.newIntent(this))
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
            } else {
                permissionManager.requestPermissions()
            }
        }
    }

    /** Cập nhật trạng thái navigation dựa trên tab được chọn */
    private fun updateNavigationState(itemId: Int) {
        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }

        val transition = Fade()
        transition.duration = 120
        TransitionManager.beginDelayedTransition(binding.mainContent, transition)

        binding.homeContainer.root.visibility = View.GONE
        binding.historyContainer.visibility = View.GONE
        binding.myGardenContainer.visibility = View.GONE
        binding.settingsContainer.root.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.fabSpeak.visibility = View.GONE
        binding.fabMute.visibility = View.GONE

        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fabCamera.visibility = View.VISIBLE

        when (itemId) {
            R.id.nav_home -> {
                binding.homeContainer.root.visibility = View.VISIBLE
                binding.searchBarContainer.visibility = View.VISIBLE

                val state = viewModel.uiState.value
                val isComplete = state.loadingStage == LoadingStage.COMPLETE
                val hasInfo = state.speciesInfo != null && !state.isLoading && state.error == null

                if (isComplete && hasInfo && !speakerManager.isSpeaking()) {
                    binding.fabSpeak.visibility = View.VISIBLE
                } else if (speakerManager.isSpeaking()) {
                    binding.fabMute.visibility = View.VISIBLE
                }
            }
            R.id.nav_history -> {
                binding.historyContainer.visibility = View.VISIBLE
                if (!historyFragment.isAdded) {
                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.historyContainer, historyFragment, "HISTORY")
                            .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_my_garden -> {
                binding.myGardenContainer.visibility = View.VISIBLE
                if (!chatHistoryFragment.isAdded) {
                    supportFragmentManager
                            .beginTransaction()
                            .add(R.id.myGardenContainer, chatHistoryFragment, "CHAT_HISTORY")
                            .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_settings -> binding.settingsContainer.root.visibility = View.VISIBLE
        }
    }

    /** Thiết lập các Floating Action Button */
    private fun setupFAB() {
        // Camera FAB logic moved to setupBottomNavigation to ensure it works for both instances if needed
        // But since we have one ID, we just need to make sure we set the listener correctly.
        // The listener is set in setupBottomNavigation now.

        binding.fabSpeak.setOnClickListener {
            viewModel.uiState.value.speciesInfo?.let { info ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val text = TextToSpeechGenerator.generateSpeechText(this@MainActivity, info)
                    if (text.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            speakerManager.setLanguage(languageManager.getLanguage())
                            speakerManager.speak(text)
                            toggleSpeakerUI(true)
                        }
                    }
                }
            }
        }

        binding.fabMute.setOnClickListener {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }
    }

    /** Chuyển đổi UI của speaker button */
    private fun toggleSpeakerUI(isSpeaking: Boolean) {
        if (!binding.homeContainer.root.isVisible) return
        binding.fabSpeak.visibility = if (!isSpeaking) View.VISIBLE else View.GONE
        binding.fabMute.visibility = if (isSpeaking) View.VISIBLE else View.GONE
    }

    /** Thiết lập observers cho UI state */
    private fun setupObservers() {
        lifecycleScope.launch { viewModel.uiState.collect { state -> updateHomeUI(state) } }
    }

    /** Cập nhật UI màn hình Home dựa trên state */
    private suspend fun updateHomeUI(state: com.nguyendevs.ecolens.model.EcoLensUiState) {
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
        val progressBarHero = homeRoot.findViewById<View>(R.id.progressBarHero)
        val tvLoadingText = homeRoot.findViewById<TextView>(R.id.tvLoadingText)
        val loadingCard = homeRoot.findViewById<View>(R.id.loadingCard)
        
        // Hide legacy loading card
        loadingCard?.isVisible = false

        // Ensure we are in the correct state
        if (isLoading || state.speciesInfo != null || error != null) {
            toggleHomeState(showResults = true)
        } else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null && error == null
        ) {
            // Reset to home if nothing is happening
            toggleHomeState(showResults = false)
        }

        // Manage Progress Bar and Loading Text
        if (isLoading) {
            progressBarHero?.isVisible = true
            tvLoadingText?.isVisible = true
            
            stopLoadingJob?.cancel()

            if (loadingStage == LoadingStage.SCIENTIFIC_NAME ||
                            loadingStage == LoadingStage.COMMON_NAME ||
                            loadingStage == LoadingStage.TAXONOMY
            ) {
                loadingAnimationHandler.setText(R.string.analyzing_info)
            } else {
                loadingAnimationHandler.setText(R.string.analyzing_text)
            }

            loadingAnimationHandler.start()
        } else {
            // Not loading
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
            homeRoot.findViewById<TextView>(R.id.errorText).text = error
            val errorCard = homeRoot.findViewById<View>(R.id.errorCard)
            if (errorCard != null) errorCard.isVisible = true
            
            progressBarHero?.isVisible = false
            tvLoadingText?.isVisible = false

            binding.homeContainer.speciesInfoCard.root.isVisible = false
            binding.fabSpeak.isVisible = false

            // Allow retry or reset
            binding.fabCamera.isClickable = true
            binding.fabCamera.alpha = 1.0f
        } else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = false
            homeRoot.findViewById<View>(R.id.errorCard).isVisible = false
            binding.fabSpeak.isVisible = false
            speciesInfoHandler.displaySpeciesInfo(
                    SpeciesInfo(scientificName = "", commonName = ""),
                    LoadingStage.NONE
            )
        } else if (state.speciesInfo != null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = true
            val errorCard = homeRoot.findViewById<View>(R.id.errorCard)
            if (errorCard != null) errorCard.isVisible = false

            // Pass imageUri to display it in the card if feasible
            speciesInfoHandler.displaySpeciesInfo(state.speciesInfo, loadingStage)

            if (loadingStage == LoadingStage.COMPLETE && binding.fabMute.visibility != View.VISIBLE
            ) {
                binding.fabSpeak.isVisible = true
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
        speakerManager.shutdown()
        speciesInfoHandler.onDestroy()
        super.onDestroy()
    }

    /** Thiết lập xử lý nút Back */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
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
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        )
    }

    // ==================== HOME SCREEN SETUP ====================

    /**
     * Thiết lập các components mới cho Home Screen Bao gồm: greeting động, hero card button, và
     * Recent History
     */
    private fun setupHomeScreen() {
        val homeRoot = binding.homeContainer.root

        // Setup dynamic greeting based on time of day
        setupGreeting()

        // Setup Hero Card button click
        homeRoot.findViewById<View>(R.id.btnStartNow)?.setOnClickListener {
            binding.fabCamera.performClick()
        }

        // Setup Recent History RecyclerView
        setupRecentHistory()

        // Setup Quick Explore hardcoded data
        setupQuickExplore()
    }

    /** Thiết lập lời chào động theo thời gian trong ngày + Username */
    private fun setupGreeting() {
        val homeRoot = binding.homeContainer.root
        val tvGreeting = homeRoot.findViewById<TextView>(R.id.tvGreeting) ?: return

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingResId =
                when {
                    hour < 12 -> R.string.greeting_morning
                    hour < 18 -> R.string.greeting_afternoon
                    else -> R.string.greeting_evening
                }

        val username = sharedPreferences.getString("username", "")
        if (username.isNullOrEmpty()) {
            tvGreeting.setText(greetingResId)
        } else {
            // Combine resource string with username
            val greetingBase = getString(greetingResId)
            tvGreeting.text = "$greetingBase, $username"
        }
    }

    /** Thiết lập RecyclerView cho Recent History với 5 item gần nhất */
    private fun setupRecentHistory() {
        val homeRoot = binding.homeContainer.root
        val rvRecentHistory =
                homeRoot.findViewById<androidx.recyclerview.widget.RecyclerView>(
                        R.id.rvRecentHistory
                )
                        ?: return
        val emptyRecentState = homeRoot.findViewById<View>(R.id.emptyRecentState)

        // Initialize adapter with click handler
        recentHistoryAdapter = RecentHistoryAdapter { entry ->
            // Navigate to History Detail when clicking on recent item
            navigateToHistoryDetail(entry)
        }

        rvRecentHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentHistoryAdapter
            isNestedScrollingEnabled = false
        }

        // Load 5 most recent items
        loadRecentHistory(emptyRecentState)
    }

    /** Load 5 item lịch sử gần nhất từ database */
    private fun loadRecentHistory(emptyState: View?) {
        lifecycleScope.launch {
            historyRepository.getAllHistoryNewestFirst().collect { allHistory ->
                val recentItems = allHistory.take(5)
                recentHistoryAdapter.submitList(recentItems)

                // Toggle empty state
                emptyState?.isVisible = recentItems.isEmpty()
            }
        }
    }

    /** Navigate to History Detail Fragment */
    private fun navigateToHistoryDetail(entry: com.nguyendevs.ecolens.model.history.HistoryEntry) {
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

    /** Setup Quick Explore với dữ liệu hardcoded */
    private fun setupQuickExplore() {
        val homeRoot = binding.homeContainer.root

        // Card 1: Sen đá
        val card1 = homeRoot.findViewById<View>(R.id.exploreCard1)
        card1?.findViewById<TextView>(R.id.tvExploreName)?.text =
                getString(R.string.explore_item_1_name)
        card1?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                getString(R.string.explore_item_1_desc)
        card1?.findViewById<ImageView>(R.id.imgExplore)?.setImageResource(R.drawable.home_tree)

        // Card 2: Bướm Monarch
        val card2 = homeRoot.findViewById<View>(R.id.exploreCard2)
        card2?.findViewById<TextView>(R.id.tvExploreName)?.text =
                getString(R.string.explore_item_2_name)
        card2?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                getString(R.string.explore_item_2_desc)
        card2?.findViewById<ImageView>(R.id.imgExplore)?.setImageResource(R.drawable.home_tree_2)

        // Card 3: Hoa Oải Hương
        val card3 = homeRoot.findViewById<View>(R.id.exploreCard3)
        card3?.findViewById<TextView>(R.id.tvExploreName)?.text =
                getString(R.string.explore_item_3_name)
        card3?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                getString(R.string.explore_item_3_desc)
        card3?.findViewById<ImageView>(R.id.imgExplore)?.setImageResource(R.drawable.home_tree)
    }
}
