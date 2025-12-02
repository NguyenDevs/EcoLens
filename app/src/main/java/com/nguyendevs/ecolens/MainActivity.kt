package com.nguyendevs.ecolens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.activity.CameraActivity
import com.nguyendevs.ecolens.activity.HistoryDetailFragment
import com.nguyendevs.ecolens.adapter.HistoryAdapter
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.SpeciesInfo
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
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View
    private lateinit var loadingCard: MaterialCardView
    private lateinit var errorCard: MaterialCardView
    private lateinit var errorText: TextView
    private lateinit var speciesInfoCard: MaterialCardView

    // History screen views
    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var viewModel: EcoLensViewModel
    private var imageUri: Uri? = null

    // Nhận kết quả từ CameraActivity
    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                val capturedUri = Uri.parse(uriString)
                imageUri = capturedUri

                // Hiển thị ảnh preview ở Home
                Glide.with(this)
                    .load(capturedUri)
                    .centerCrop()
                    .into(imagePreview)

                // Bắt đầu nhận diện
                viewModel.identifySpecies(capturedUri)
            }
        }
    }

    // Nhận kết quả từ thư viện ảnh (nút Upload)
    // Lưu ý: Logic upload nằm trong CameraActivity, MainActivity chỉ cần xử lý ảnh trả về từ CameraActivity
    // Tuy nhiên nếu bạn gọi gallery trực tiếp từ MainActivity thì dùng launcher này
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this)
                .load(it)
                .centerCrop()
                .into(imagePreview)

            viewModel.identifySpecies(it)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cần quyền truy cập")
                .setMessage("Ứng dụng cần quyền camera và thư viện ảnh để hoạt động.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupViewModel()
        setupBottomNavigation()
        setupHistoryScreen()
        setupFAB()
        setupObservers()

        // Mặc định hiển thị Home
        showHomeScreen()
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
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingCard = findViewById(R.id.loadingCard)
        errorCard = findViewById(R.id.errorCard)
        errorText = findViewById(R.id.errorText)
        speciesInfoCard = findViewById(R.id.speciesInfoCard)

        // History screen views
        rvHistory = historyContainer.findViewById(R.id.rvHistory)
        emptyStateContainer = historyContainer.findViewById(R.id.emptyStateContainer)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[EcoLensViewModel::class.java]
    }

    private fun setupHistoryScreen() {
        // [QUAN TRỌNG] Sửa callback ở đây: Khi click item -> mở Fragment chi tiết
        historyAdapter = HistoryAdapter(emptyList()) { entry ->
            openHistoryDetail(entry)
        }

        rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    // Hàm mở Fragment chi tiết lịch sử
    fun openHistoryDetail(entry: HistoryEntry) {
        val detailFragment = HistoryDetailFragment()
        detailFragment.setData(entry)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.hold, R.anim.hold, R.anim.slide_out_bottom)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("history_detail")
            .commit()
    }

    private fun setupBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showHomeScreen()
                R.id.nav_history -> showHistoryScreen()
                R.id.nav_my_garden -> showMyGardenScreen()
                R.id.nav_settings -> showSettingsScreen()
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun setupFAB() {
        // Nút search ở màn hình Home (để chụp ảnh)
        findViewById<FloatingActionButton>(R.id.fabSearch).setOnClickListener {
            checkPermissionsAndOpenCameraActivity()
        }
        // Nút camera chính
        findViewById<FloatingActionButton>(R.id.fabCamera).setOnClickListener {
            checkPermissionsAndOpenCameraActivity()
        }
    }

    private fun checkPermissionsAndOpenCameraActivity() {
        if (hasPermissions()) {
            openCameraActivity()
        } else {
            requestPermissions()
        }
    }

    private fun openCameraActivity() {
        cameraActivityLauncher.launch(CameraActivity.newIntent(this))
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold)
    }

    private fun hasPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return camera && storage
    }

    private fun requestPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Chỉ update giao diện Home nếu đang ở tab Home
                if (homeContainer.visibility == View.VISIBLE) {
                    loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    loadingCard.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.error != null) {
                        errorText.text = state.error
                        errorCard.visibility = View.VISIBLE
                        speciesInfoCard.visibility = View.GONE
                    } else {
                        errorCard.visibility = View.GONE
                    }

                    state.speciesInfo?.let { info ->
                        displaySpeciesInfo(info)
                        speciesInfoCard.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observer cho History List
        lifecycleScope.launch {
            viewModel.history.collect { historyList ->
                historyAdapter.updateList(historyList)
                if (historyList.isNotEmpty()) {
                    rvHistory.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE
                } else {
                    rvHistory.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun displaySpeciesInfo(info: SpeciesInfo) {
        speciesInfoCard.findViewById<TextView>(R.id.tvCommonName)?.text = info.commonName
        speciesInfoCard.findViewById<TextView>(R.id.tvScientificName)?.text = info.scientificName
        speciesInfoCard.findViewById<TextView>(R.id.tvConfidence)?.text =
            "Độ tin cậy: ${(info.confidence * 100).toInt()}%"

        setTextIfNotEmpty(R.id.tvKingdom, info.kingdom)
        setTextIfNotEmpty(R.id.tvPhylum, info.phylum)
        setTextIfNotEmpty(R.id.tvClass, info.className)
        setTextIfNotEmpty(R.id.tvOrder, info.order)
        setTextIfNotEmpty(R.id.tvFamily, info.family)
        setTextIfNotEmpty(R.id.tvGenus, info.genus)
        setTextIfNotEmpty(R.id.tvSpecies, info.species)

        setSectionVisibility(R.id.sectionDescription, R.id.tvDescription, info.description, "Không có thông tin chi tiết")
        setSectionVisibility(R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics)
        setSectionVisibility(R.id.sectionDistribution, R.id.tvDistribution, info.distribution)
        setSectionVisibility(R.id.sectionHabitat, R.id.tvHabitat, info.habitat)
    }

    private fun setTextIfNotEmpty(viewId: Int, text: String) {
        speciesInfoCard.findViewById<TextView>(viewId)?.apply {
            if (text.isNotEmpty()) {
                this.text = text
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun setSectionVisibility(
        sectionId: Int,
        textViewId: Int,
        text: String,
        excludeText: String? = null
    ) {
        val section = speciesInfoCard.findViewById<LinearLayout>(sectionId)
        val textView = speciesInfoCard.findViewById<TextView>(textViewId)

        if (text.isNotEmpty() && text != excludeText) {
            textView?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(text)
            }
            section?.visibility = View.VISIBLE
        } else {
            section?.visibility = View.GONE
        }
    }

    // Các hàm chuyển màn hình
    private fun showHomeScreen() {
        fabSearch.visibility = View.VISIBLE
        homeContainer.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    private fun showHistoryScreen() {
        fabSearch.visibility = View.GONE // Ẩn nút search ở màn hình History
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.VISIBLE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    private fun showMyGardenScreen() {
        fabSearch.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.VISIBLE
        settingsContainer.visibility = View.GONE
    }

    private fun showSettingsScreen() {
        fabSearch.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.VISIBLE
    }
}