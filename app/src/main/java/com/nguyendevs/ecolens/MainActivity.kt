package com.nguyendevs.ecolens

import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.fragments.ChatHistoryFragment
import com.nguyendevs.ecolens.fragments.HistoryFragment
import com.nguyendevs.ecolens.handlers.*
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var errorCard: View
    private lateinit var errorText: TextView
    private lateinit var fabMute: FloatingActionButton
    private lateinit var fabSpeak: FloatingActionButton
    private lateinit var fabCamera: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var homeContainer: View
    private lateinit var myGardenContainer: FrameLayout
    private lateinit var imagePreview: ImageView
    private lateinit var imagePreviewCard: MaterialCardView
    private lateinit var initialStateLayout: View
    private lateinit var imageZoomHandler: ImageZoomHandler
    private lateinit var languageManager: LanguageManager
    private lateinit var loadingAnimationHandler: LoadingAnimationHandler
    private lateinit var loadingCard: View
    private lateinit var loadingOverlay: View
    private lateinit var mainContent: ViewGroup
    private lateinit var overlayContainer: FrameLayout
    private lateinit var permissionManager: PermissionManager
    private lateinit var searchBarContainer: View
    private lateinit var searchBarHandler: SearchBarHandler
    private lateinit var settingsContainer: View
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var speakerManager: SpeakerManager
    private lateinit var speciesInfoCard: MaterialCardView
    private lateinit var speciesInfoHandler: SpeciesInfoHandler
    private lateinit var viewModel: EcoLensViewModel

    private val historyFragment = HistoryFragment()
    private val chatHistoryFragment = ChatHistoryFragment()
    private var imageUri: Uri? = null
    private var isExpandedState = false
    private var stopLoadingJob: Job? = null

    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                handleCapturedImage(Uri.parse(uriString))
            }
        }
    }

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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_modern)

        initViews()
        setupViewModel()
        initHandlers()
        initManagers()
        setupBottomNavigation()
        setupFAB()
        setupObservers()

        updateNavigationState(R.id.nav_home)
        preloadFragments()
    }

    private fun initViews() {
        mainContent = findViewById(R.id.mainContent)
        homeContainer = findViewById(R.id.homeContainer)
        fragmentContainer = findViewById(R.id.historyContainer)
        myGardenContainer = findViewById(R.id.myGardenContainer)
        overlayContainer = findViewById(R.id.fragmentContainer)

        settingsContainer = findViewById(R.id.settingsContainer)
        searchBarContainer = findViewById(R.id.searchBarContainer)

        fabSpeak = findViewById(R.id.fabSpeak)
        fabMute = findViewById(R.id.fabMute)
        fabCamera = findViewById(R.id.fabCamera)
        bottomNav = findViewById(R.id.bottomNavigation)

        imagePreviewCard = findViewById(R.id.imagePreviewCard)
        imagePreview = findViewById(R.id.imagePreview)
        initialStateLayout = findViewById(R.id.initialStateLayout)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingCard = findViewById(R.id.loadingCard)
        errorCard = findViewById(R.id.errorCard)
        errorText = findViewById(R.id.errorText)
        speciesInfoCard = findViewById(R.id.speciesInfoCard)

    }

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
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EcoLensViewModel::class.java]
    }

    private fun initHandlers() {
        settingsHandler = SettingsHandler(this, languageManager, settingsContainer)
        settingsHandler.setup()

        searchBarHandler = SearchBarHandler(
            this,
            searchBarContainer as MaterialCardView,
            findViewById(R.id.textInputLayoutSearch),
            findViewById(R.id.etSearchQuery),
            findViewById(R.id.btnSearchAction)
        )
        searchBarHandler.setup()

        imageZoomHandler = ImageZoomHandler(
            findViewById(R.id.btnZoomIn),
            findViewById(R.id.btnZoomOut),
            findViewById(R.id.fullScreenContainer),
            findViewById(R.id.fullScreenImage)
        )
        imageZoomHandler.setup()

        loadingAnimationHandler = LoadingAnimationHandler(
            loadingCard.findViewById(R.id.tvLoading),
            lifecycleScope
        )
    }

    private fun initManagers() {
        permissionManager = PermissionManager(this, permissionLauncher)
        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = {
            runOnUiThread { toggleSpeakerUI(false) }
        }
        speciesInfoHandler = SpeciesInfoHandler(
            this,
            speciesInfoCard,
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



        supportFragmentManager.addOnBackStackChangedListener {
            val count = supportFragmentManager.backStackEntryCount
            if (count > 0) {
                overlayContainer.visibility = View.VISIBLE
            } else {
                overlayContainer.postDelayed({
                    if (supportFragmentManager.backStackEntryCount == 0) {
                        overlayContainer.visibility = View.GONE
                    }
                }, 400)
            }
        }
    }

    private fun handleCapturedImage(uri: Uri) {
        fabCamera.isClickable = false
        fabCamera.alpha = 0.5f
        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }

        if (searchBarHandler.isExpanded()) searchBarHandler.collapseSearchBar()

        bottomNav.selectedItemId = R.id.nav_home

        imageUri = uri

        animateCardExpansion {
            Glide.with(this).load(uri).centerCrop().into(imagePreview)
            imageZoomHandler.setImageUri(uri)
            viewModel.identifySpecies(uri, languageManager.getLanguage())
        }
    }

    private fun animateCardExpansion(onAnimationComplete: () -> Unit) {
        if (isExpandedState) {
            onAnimationComplete()
            return
        }

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

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }

            updateNavigationState(item.itemId)
            true
        }
    }

    private fun updateNavigationState(itemId: Int) {
        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }

        val transition = Fade()
        transition.duration = 120
        TransitionManager.beginDelayedTransition(mainContent, transition)

        homeContainer.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE

        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        fabMute.visibility = View.GONE

        bottomNav.visibility = View.VISIBLE
        fabCamera.visibility = View.VISIBLE

        when (itemId) {
            R.id.nav_home -> {
                homeContainer.visibility = View.VISIBLE
                searchBarContainer.visibility = View.VISIBLE

                val state = viewModel.uiState.value
                val isComplete = state.loadingStage == LoadingStage.COMPLETE
                val hasInfo = state.speciesInfo != null && !state.isLoading && state.error == null

                if (isComplete && hasInfo && !speakerManager.isSpeaking()) {
                    fabSpeak.visibility = View.VISIBLE
                } else if (speakerManager.isSpeaking()) {
                    fabMute.visibility = View.VISIBLE
                }
            }
            R.id.nav_history -> {
                fragmentContainer.visibility = View.VISIBLE
                if (!historyFragment.isAdded) {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.historyContainer, historyFragment, "HISTORY")
                        .commitNowAllowingStateLoss()
                }
                /*
                val transaction = supportFragmentManager.beginTransaction()
                if (historyFragment == null) {
                    historyFragment = HistoryFragment()
                    transaction.add(R.id.historyContainer, historyFragment!!, "HISTORY")
                }
                transaction.commitNowAllowingStateLoss()
                 */
            }
            R.id.nav_my_garden -> {
                myGardenContainer.visibility = View.VISIBLE
                if (!chatHistoryFragment.isAdded) {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.myGardenContainer, chatHistoryFragment, "CHAT_HISTORY")
                        .commitNowAllowingStateLoss()
                }
                /*
                val transaction = supportFragmentManager.beginTransaction()
                if (chatHistoryFragment == null) {
                    chatHistoryFragment = ChatHistoryFragment()
                    transaction.add(R.id.myGardenContainer, chatHistoryFragment!!, "CHAT_HISTORY")
                }
                transaction.commitNowAllowingStateLoss()
                 */
            }
            R.id.nav_settings -> settingsContainer.visibility = View.VISIBLE
        }
    }

    private fun setupFAB() {
        fabCamera.setOnClickListener {
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

        fabSpeak.setOnClickListener {
            viewModel.uiState.value.speciesInfo?.let { info ->
                val text = TextToSpeechGenerator.generateSpeechText(this, info)
                if (text.isNotEmpty()) {
                    speakerManager.setLanguage(languageManager.getLanguage())
                    speakerManager.speak(text)
                    toggleSpeakerUI(true)
                }
            }
        }

        fabMute.setOnClickListener {
            speakerManager.pause()
            toggleSpeakerUI(false)
        }
    }

    private fun toggleSpeakerUI(isSpeaking: Boolean) {
        if (homeContainer.visibility != View.VISIBLE) return
        fabSpeak.visibility = if (!isSpeaking) View.VISIBLE else View.GONE
        fabMute.visibility = if (isSpeaking) View.VISIBLE else View.GONE
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (homeContainer.visibility == View.VISIBLE) {
                    updateHomeUI(state)
                }
            }
        }
    }

    private suspend fun updateHomeUI(state: com.nguyendevs.ecolens.model.EcoLensUiState) {
        val isLoading = state.isLoading
        val error = state.error
        val loadingStage = state.loadingStage
        fabCamera.isClickable = !isLoading
        fabCamera.alpha = if (isLoading) 0.5f else 1.0f
        loadingOverlay.isVisible = isLoading
        loadingCard.isVisible = isLoading

        if (isLoading){
            stopLoadingJob?.cancel()
            loadingAnimationHandler.start()
        }
        else {
            stopLoadingJob?.cancel()
            stopLoadingJob = coroutineScope {
                launch {
                    delay(500)
                    loadingAnimationHandler.stop()
                }
            }
        }

        if (error != null) {
            errorText.text = error
            errorCard.isVisible = true
            speciesInfoCard.isVisible = false
            fabSpeak.isVisible = false

            if (::initialStateLayout.isInitialized && initialStateLayout.visibility == View.VISIBLE) {
                initialStateLayout.visibility = View.GONE
            }
        }
        else if (loadingStage == LoadingStage.NONE && state.speciesInfo == null) {
            speciesInfoCard.isVisible = false
            errorCard.isVisible = false
            fabSpeak.isVisible = false
            speciesInfoHandler.displaySpeciesInfo(
                SpeciesInfo(scientificName = "", commonName = ""),
                null,
                LoadingStage.NONE
            )
        }
        else if (state.speciesInfo != null) {
            speciesInfoCard.isVisible = true
            errorCard.isVisible = false

            speciesInfoHandler.displaySpeciesInfo(state.speciesInfo, imageUri, loadingStage)

            if (loadingStage == LoadingStage.COMPLETE && fabMute.visibility != View.VISIBLE) {
                fabSpeak.isVisible = true
            } else {
                fabSpeak.isVisible = false
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

    override fun onBackPressed() {
        if (imageZoomHandler.isFullScreenVisible()) {
            imageZoomHandler.hideFullScreen()
            return
        }

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return
        }

        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}