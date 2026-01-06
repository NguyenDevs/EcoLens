package com.nguyendevs.ecolens

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.databinding.ActivityMainModernBinding
import com.nguyendevs.ecolens.fragments.ChatHistoryFragment
import com.nguyendevs.ecolens.fragments.HistoryFragment
import com.nguyendevs.ecolens.handlers.*
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.*
import java.io.File

/**
 * Activity chính của ứng dụng EcoLens
 * Quản lý navigation, nhận diện loài, và các tính năng chính
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainModernBinding
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

    companion object {
        private const val PREF_NAME = "EcoLensPrefs"
        private const val KEY_LAST_NAV_ITEM = "last_nav_item"
        var transitionBitmap: Bitmap? = null
    }

    /**
     * Launcher cho CameraActivity
     */
    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                handleCapturedImage(uriString.toUri())
            }
        }
    }

    /**
     * Launcher cho yêu cầu quyền
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
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

        try {
            FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL).setPersistenceEnabled(true)
        } catch (e: Exception) {
        }

        binding = ActivityMainModernBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Xử lý transition animation từ splash screen
        if (transitionBitmap != null) {
            val rootView = window.decorView as ViewGroup
            val overlay = ImageView(this)
            overlay.setImageBitmap(transitionBitmap)
            overlay.scaleType = ImageView.ScaleType.FIT_XY
            overlay.elevation = 100f
            overlay.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            rootView.addView(overlay)

            overlay.animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    rootView.removeView(overlay)
                    if (transitionBitmap != null && !transitionBitmap!!.isRecycled) {
                        transitionBitmap!!.recycle()
                    }
                    transitionBitmap = null
                }
                .start()
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setupViewModel()

        // Khôi phục trạng thái ảnh nếu có
        viewModel.currentImageUri?.let { uri ->
            this.imageUri = uri
            binding.root.post {
                restoreExpandedState(uri)
            }
        }

        initHandlers()
        initManagers()
        setupBottomNavigation()
        setupFAB()
        setupObservers()
        setupBackNavigation()

        // Điều hướng đến tab phù hợp
        val navigateToSettings = intent.getBooleanExtra("navigate_to_settings", false)
        val lastNavItem = if (navigateToSettings) {
            R.id.nav_settings
        } else {
            sharedPreferences.getInt(KEY_LAST_NAV_ITEM, R.id.nav_home)
        }

        binding.bottomNavigation.selectedItemId = lastNavItem
        binding.root.post {
            updateNavigationState(lastNavItem)
        }

        preloadFragments()
    }

    /**
     * Preload các fragment để cải thiện hiệu suất
     */
    private fun preloadFragments() {
        lifecycleScope.launch {
            delay(500)
            if (!isDestroyed) {
                val transaction = supportFragmentManager.beginTransaction()
                if (!historyFragment.isAdded) {
                    transaction.add(R.id.historyContainer, historyFragment, "HISTORY")
                }
                if (!chatHistoryFragment.isAdded) {
                    transaction.add(R.id.myGardenContainer, chatHistoryFragment, "CHAT_HISTORY")
                }
                transaction.commitAllowingStateLoss()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /**
     * Tải cài đặt theme từ SharedPreferences
     */
    private fun loadThemePreference() {
        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Khởi tạo ViewModel
     */
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EcoLensViewModel::class.java]
    }

    /**
     * Khởi tạo các handler xử lý UI
     */
    private fun initHandlers() {
        settingsHandler = SettingsHandler(this, languageManager, binding.settingsContainer.root)

        val homeRoot = binding.homeContainer.root

        searchBarHandler = SearchBarHandler(
            this,
            binding.searchBarContainer,
            binding.textInputLayoutSearch,
            binding.etSearchQuery,
            binding.btnSearchAction
        )

        imageZoomHandler = ImageZoomHandler(
            homeRoot.findViewById(R.id.btnZoomIn),
            binding.btnZoomOut,
            binding.fullScreenContainer,
            binding.fullScreenImage
        )

        loadingAnimationHandler = LoadingAnimationHandler(
            homeRoot.findViewById(R.id.tvLoading),
            lifecycleScope
        )

        speciesInfoHandler = SpeciesInfoHandler(
            this,
            binding,
            onCopySuccess = { copiedText ->
                searchBarHandler.expandSearchBar(copiedText)
            },
            onRetryClick = {
                if (speakerManager.isSpeaking()) {
                    speakerManager.pause()
                    toggleSpeakerUI(false)
                }
                viewModel.retryIdentification()
            }
        )
    }

    /**
     * Khởi tạo các manager
     */
    private fun initManagers() {
        permissionManager = PermissionManager(this, permissionLauncher)
        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = {
            runOnUiThread { toggleSpeakerUI(false) }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val count = supportFragmentManager.backStackEntryCount
            if (count > 0) {
                binding.fragmentContainer.visibility = View.VISIBLE
            } else {
                binding.fragmentContainer.postDelayed({
                    if (supportFragmentManager.backStackEntryCount == 0) {
                        binding.fragmentContainer.visibility = View.GONE
                    }
                }, 400)
            }
        }
    }

    /**
     * Xử lý ảnh đã chụp từ camera
     */
    private fun handleCapturedImage(uri: Uri) {
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

        animateCardExpansion {
            val homeRoot = binding.homeContainer.root
            val imagePreview = homeRoot.findViewById<View>(R.id.imagePreview)

            Glide.with(this).load(uri).centerCrop().into(imagePreview as android.widget.ImageView)
            imageZoomHandler.setImageUri(uri)
            viewModel.identifySpecies(uri, languageManager.getLanguage())
        }
    }

    /**
     * Khôi phục trạng thái đã mở rộng với ảnh
     */
    private fun restoreExpandedState(uri: Uri) {
        isExpandedState = true

        val homeRoot = binding.homeContainer.root
        val imagePreviewCard = homeRoot.findViewById<View>(R.id.imagePreviewCard)
        val initialStateLayout = homeRoot.findViewById<View>(R.id.initialStateLayout)
        val imagePreview = homeRoot.findViewById<View>(R.id.imagePreview)

        initialStateLayout.visibility = View.GONE
        initialStateLayout.alpha = 0f

        val targetHeight = (290 * resources.displayMetrics.density).toInt()
        val params = imagePreviewCard.layoutParams
        params.height = targetHeight
        imagePreviewCard.layoutParams = params

        imagePreview.visibility = View.VISIBLE
        imagePreview.alpha = 1f

        val path = uri.path
        val loadModel = if (path != null) {
            val file = File(path)
            if (file.exists()) file else uri
        } else {
            uri
        }

        Glide.with(this).load(loadModel).centerCrop().into(imagePreview as android.widget.ImageView)

        imageZoomHandler.setImageUri(uri)

        if (searchBarHandler.isExpanded()) searchBarHandler.collapseSearchBar()
    }

    /**
     * Animation mở rộng card hiển thị ảnh
     */
    private fun animateCardExpansion(onAnimationComplete: () -> Unit) {
        if (isExpandedState) {
            onAnimationComplete()
            return
        }

        val homeRoot = binding.homeContainer.root
        val imagePreviewCard = homeRoot.findViewById<View>(R.id.imagePreviewCard)
        val initialStateLayout = homeRoot.findViewById<View>(R.id.initialStateLayout)
        val imagePreview = homeRoot.findViewById<View>(R.id.imagePreview)

        val startHeight = imagePreviewCard.height
        val targetHeight = (290 * resources.displayMetrics.density).toInt()

        initialStateLayout.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                initialStateLayout.visibility = View.GONE

                val heightAnimator = ValueAnimator.ofInt(startHeight, targetHeight)
                heightAnimator.duration = 400
                heightAnimator.interpolator = AccelerateDecelerateInterpolator()
                heightAnimator.addUpdateListener { animator ->
                    val params = imagePreviewCard.layoutParams
                    params.height = animator.animatedValue as Int
                    imagePreviewCard.layoutParams = params
                }

                heightAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        isExpandedState = true
                        imagePreview.visibility = View.VISIBLE
                        imagePreview.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                        onAnimationComplete()
                    }
                })
                heightAnimator.start()
            }
            .start()
    }

    /**
     * Thiết lập bottom navigation
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.bottomNavigation.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }

            sharedPreferences.edit().putInt(KEY_LAST_NAV_ITEM, item.itemId).apply()

            updateNavigationState(item.itemId)
            true
        }
    }

    /**
     * Cập nhật trạng thái navigation dựa trên tab được chọn
     */
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
                    supportFragmentManager.beginTransaction()
                        .add(R.id.historyContainer, historyFragment, "HISTORY")
                        .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_my_garden -> {
                binding.myGardenContainer.visibility = View.VISIBLE
                if (!chatHistoryFragment.isAdded) {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.myGardenContainer, chatHistoryFragment, "CHAT_HISTORY")
                        .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_settings -> binding.settingsContainer.root.visibility = View.VISIBLE
        }
    }

    /**
     * Thiết lập các Floating Action Button
     */
    private fun setupFAB() {
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

    /**
     * Chuyển đổi UI của speaker button
     */
    private fun toggleSpeakerUI(isSpeaking: Boolean) {
        if (!binding.homeContainer.root.isVisible) return
        binding.fabSpeak.visibility = if (!isSpeaking) View.VISIBLE else View.GONE
        binding.fabMute.visibility = if (isSpeaking) View.VISIBLE else View.GONE
    }

    /**
     * Thiết lập observers cho UI state
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateHomeUI(state)
            }
        }
    }

    /**
     * Cập nhật UI màn hình Home dựa trên state
     */
    private suspend fun updateHomeUI(state: com.nguyendevs.ecolens.model.EcoLensUiState) {
        val isLoading = state.isLoading
        val error = state.error
        val loadingStage = state.loadingStage

        val isPhase2 = loadingStage == LoadingStage.DESCRIPTION ||
                loadingStage == LoadingStage.CHARACTERISTICS ||
                loadingStage == LoadingStage.DISTRIBUTION ||
                loadingStage == LoadingStage.HABITAT ||
                loadingStage == LoadingStage.CONSERVATION

        val showOverlay = isLoading && !isPhase2

        binding.fabCamera.isClickable = !isLoading
        binding.fabCamera.alpha = if (isLoading) 0.5f else 1.0f

        val homeRoot = binding.homeContainer.root
        val loadingOverlay = homeRoot.findViewById<View>(R.id.loadingOverlay)
        loadingOverlay.isVisible = showOverlay

        homeRoot.findViewById<View>(R.id.loadingCard).isVisible = showOverlay

        if (showOverlay) {
            stopLoadingJob?.cancel()

            if (loadingStage == LoadingStage.SCIENTIFIC_NAME ||
                loadingStage == LoadingStage.COMMON_NAME ||
                loadingStage == LoadingStage.TAXONOMY) {
                loadingAnimationHandler.setText(R.string.analyzing_info)
            } else {
                loadingAnimationHandler.setText(R.string.analyzing_text)
            }

            loadingAnimationHandler.start()
        } else {
            if (isPhase2) {
                stopLoadingJob?.cancel()
                loadingAnimationHandler.stop()
            } else {
                stopLoadingJob?.cancel()
                stopLoadingJob = lifecycleScope.launch {
                    delay(500)
                    loadingAnimationHandler.stop()
                }
            }
        }

        if (error != null) {
            homeRoot.findViewById<TextView>(R.id.errorText).text = error
            homeRoot.findViewById<View>(R.id.errorCard).isVisible = true
            binding.homeContainer.speciesInfoCard.root.isVisible = false
            binding.fabSpeak.isVisible = false

            val initialStateLayout = homeRoot.findViewById<View>(R.id.initialStateLayout)
            if (initialStateLayout.visibility == View.VISIBLE) {
                initialStateLayout.visibility = View.GONE
            }
        } else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = false
            homeRoot.findViewById<View>(R.id.errorCard).isVisible = false
            binding.fabSpeak.isVisible = false
            speciesInfoHandler.displaySpeciesInfo(
                SpeciesInfo(scientificName = "", commonName = ""),
                null,
                LoadingStage.NONE
            )
        } else if (state.speciesInfo != null) {
            binding.homeContainer.speciesInfoCard.root.isVisible = true
            homeRoot.findViewById<View>(R.id.errorCard).isVisible = false

            speciesInfoHandler.displaySpeciesInfo(state.speciesInfo, imageUri, loadingStage)

            if (loadingStage == LoadingStage.COMPLETE && binding.fabMute.visibility != View.VISIBLE) {
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

    /**
     * Thiết lập xử lý nút Back
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
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
        })
    }
}