// FILE: nguyendevs/ecolens/EcoLens-312c2dae705bb34fd90d29e6d1b5003c678c945f/app/src/main/java/com/nguyendevs/ecolens/MainActivity.kt

package com.nguyendevs.ecolens

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.fragments.HistoryFragment
import com.nguyendevs.ecolens.fragments.LanguageSelectionFragment
import com.nguyendevs.ecolens.handlers.*
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.utils.KeyboardUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var mainContent: ViewGroup // Container cha để apply Transition
    private lateinit var homeContainer: View
    private lateinit var settingsContainer: View
    private lateinit var myGardenContainer: View
    // Container dành riêng cho Fragment (History)
    private lateinit var fragmentContainer: FrameLayout
    // Container đè lên tất cả để hiển thị Detail/Full Screen
    private lateinit var overlayContainer: FrameLayout

    // Home specific views
    private lateinit var imagePreview: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var loadingCard: View
    private lateinit var errorCard: View
    private lateinit var errorText: TextView
    private lateinit var speciesInfoCard: MaterialCardView
    private lateinit var fabSpeak: FloatingActionButton
    private lateinit var fabMute: FloatingActionButton
    private lateinit var searchBarContainer: View

    // --- Fragments ---
    private var historyFragment: HistoryFragment? = null

    // --- Handlers & Managers ---
    private lateinit var searchBarHandler: SearchBarHandler
    private lateinit var imageZoomHandler: ImageZoomHandler
    private lateinit var loadingAnimationHandler: LoadingAnimationHandler
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var speakerManager: SpeakerManager
    private lateinit var viewModel: EcoLensViewModel
    private lateinit var permissionManager: PermissionManager
    private lateinit var speciesInfoHandler: SpeciesInfoHandler
    private lateinit var languageManager: LanguageManager

    private var imageUri: Uri? = null

    // --- Launchers ---
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
        setContentView(R.layout.activity_main)

        initViews()
        initHandlers()
        initManagers()
        setupViewModel()
        setupBottomNavigation()
        setupFAB()
        setupObservers()

        // Mặc định hiển thị Home
        updateNavigationState(R.id.nav_home)
    }

    private fun initViews() {
        // Ánh xạ View từ layout activity_main.xml
        mainContent = findViewById(R.id.mainContent)
        homeContainer = findViewById(R.id.homeContainer)
        fragmentContainer = findViewById(R.id.historyContainer)
        // ID này là container full màn hình (z-index cao nhất) dùng cho DetailFragment
        overlayContainer = findViewById(R.id.fragmentContainer)

        myGardenContainer = findViewById(R.id.myGardenContainer)
        settingsContainer = findViewById(R.id.settingsContainer)
        searchBarContainer = findViewById(R.id.searchBarContainer)

        fabSpeak = findViewById(R.id.fabSpeak)
        fabMute = findViewById(R.id.fabMute)
        imagePreview = findViewById(R.id.imagePreview)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingCard = findViewById(R.id.loadingCard)
        errorCard = findViewById(R.id.errorCard)
        errorText = findViewById(R.id.errorText)
        speciesInfoCard = findViewById(R.id.speciesInfoCard)
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

        speciesInfoHandler = SpeciesInfoHandler(this, speciesInfoCard) { copiedText ->
            searchBarHandler.expandSearchBar(copiedText)
        }

        speakerManager = SpeakerManager(this)
        speakerManager.onSpeechFinished = {
            runOnUiThread { toggleSpeakerUI(false) }
        }

        // Listener để ẩn hiện overlay container khi back stack thay đổi (khi mở Detail)
        supportFragmentManager.addOnBackStackChangedListener {
            val count = supportFragmentManager.backStackEntryCount
            overlayContainer.isVisible = count > 0
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EcoLensViewModel::class.java]
    }

    private fun handleCapturedImage(uri: Uri) {
        if (searchBarHandler.isExpanded()) searchBarHandler.collapseSearchBar()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home // Chuyển về tab Home

        imageUri = uri
        Glide.with(this).load(uri).centerCrop().into(imagePreview)
        imageZoomHandler.setImageUri(uri)
        viewModel.identifySpecies(uri, languageManager.getLanguage())
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            // Xóa hết backstack (ví dụ đang xem chi tiết ở tab khác)
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            updateNavigationState(item.itemId)
            true
        }
    }

    // --- Logic chuyển Tab CÓ HOẠT ẢNH ---
    private fun updateNavigationState(itemId: Int) {
        // Tạo hiệu ứng Fade 100ms
        val transition = Fade()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(mainContent, transition)

        // 1. Ẩn tất cả container trước
        homeContainer.visibility = View.GONE
        fragmentContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE

        // Ẩn UI chung của Home
        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        fabMute.visibility = View.GONE

        // 2. Hiện container tương ứng
        when (itemId) {
            R.id.nav_home -> {
                homeContainer.visibility = View.VISIBLE
                searchBarContainer.visibility = View.VISIBLE

                // Khôi phục trạng thái nút nói
                val state = viewModel.uiState.value
                val hasInfo = state.speciesInfo != null && !state.isLoading && state.error == null
                if (hasInfo && !speakerManager.isSpeaking()) {
                    fabSpeak.visibility = View.VISIBLE
                } else if (speakerManager.isSpeaking()) {
                    fabMute.visibility = View.VISIBLE
                }
            }
            R.id.nav_history -> {
                fragmentContainer.visibility = View.VISIBLE
                val transaction = supportFragmentManager.beginTransaction()
                if (historyFragment == null) {
                    historyFragment = HistoryFragment()
                    transaction.add(R.id.historyContainer, historyFragment!!, "HISTORY")
                } else {
                    // Fragment đã được add rồi, chỉ cần layout visibility (đã xử lý ở trên)
                }
                transaction.commitNowAllowingStateLoss()
            }
            R.id.nav_my_garden -> myGardenContainer.visibility = View.VISIBLE
            R.id.nav_settings -> settingsContainer.visibility = View.VISIBLE
        }
    }

    private fun setupFAB() {
        findViewById<FloatingActionButton>(R.id.fabCamera).setOnClickListener {
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
                // Chỉ update UI khi đang ở Home
                if (homeContainer.visibility == View.VISIBLE) {
                    updateHomeUI(state)
                }
            }
        }
    }

    private fun updateHomeUI(state: com.nguyendevs.ecolens.model.EcoLensUiState) {
        val isLoading = state.isLoading
        val error = state.error

        loadingOverlay.isVisible = isLoading
        loadingCard.isVisible = isLoading

        if (isLoading) loadingAnimationHandler.start() else loadingAnimationHandler.stop()

        if (isLoading) {
            speciesInfoCard.isVisible = false
            errorCard.isVisible = false
            fabSpeak.isVisible = false
        } else if (error != null) {
            errorText.text = error
            errorCard.isVisible = true
            speciesInfoCard.isVisible = false
            fabSpeak.isVisible = false
        } else if (state.speciesInfo != null) {
            speciesInfoHandler.displaySpeciesInfo(state.speciesInfo, imageUri)
            speciesInfoCard.isVisible = true
            if (fabMute.visibility != View.VISIBLE) fabSpeak.isVisible = true
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

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}