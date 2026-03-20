package com.nguyendevs.ecolens.handlers.main

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ExploreAdapter
import com.nguyendevs.ecolens.adapters.RecentHistoryAdapter
import com.nguyendevs.ecolens.database.ExploreRepository
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.util.Calendar
import kotlinx.coroutines.launch

/** Handler khởi tạo màn hình chính: greeting, explore nhanh, lịch sử gần đây. */
class HomeScreenHandler(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val onNavigateToDetail: (HistoryEntry) -> Unit,
    private val onExploreItemClick: (String) -> Unit
) {

    private lateinit var recentHistoryAdapter: RecentHistoryAdapter
    private val historyRepository by lazy {
        HistoryRepository(
            HistoryDatabase.getDatabase(activity.applicationContext).historyDao(),
            activity.applicationContext
        )
    }
    private val exploreRepository by lazy { ExploreRepository() }
    private lateinit var exploreAdapter: ExploreAdapter
    private var hasFetchedExplore = false

    /** Lấy ngẫu nhiên 5 item explore nếu chưa fetch. */
    fun fetchExploreData() {
        if (hasFetchedExplore) return
        activity.lifecycleScope.launch {
            val items = exploreRepository.getRandomExploreItems(5)
            if (items.isNotEmpty()) {
                exploreAdapter.submitList(items)
                hasFetchedExplore = true
            }
        }
    }

    /** Khởi tạo toàn bộ góc home: greeting, nút hero, lịch sử, explore. */
    fun setup() {
        setupGreeting()
        setupHeroCardButton()
        setupRecentHistory()
        setupQuickExplore()
        fetchExploreData()
    }

    /** Thiết lập nút "Bắt đầu ngay" mở camera. */
    private fun setupHeroCardButton() {
        val homeRoot = binding.homeContainer.root
        homeRoot.findViewById<View>(R.id.btnStartNow)?.setOnClickListener {
            binding.fabCamera.performClick()
        }
    }

    /** Hiển thị lời chào theo giờ và tên người dùng từ Firebase. */
    fun setupGreeting() {
        val homeRoot = binding.homeContainer.root
        val tvGreeting = homeRoot.findViewById<TextView>(R.id.tvGreeting) ?: return

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingResId = when {
            hour < 12 -> R.string.greeting_morning
            hour < 18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }

        val greetingBase = activity.getString(greetingResId)
        tvGreeting.text = greetingBase

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        activity.lifecycleScope.launch {
            try {
                val userDetails = UserRepository().getCurrentUserDetails()
                val username = userDetails?.username?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }

                if (!username.isNullOrEmpty()) {
                    tvGreeting.text = "$greetingBase $username"
                }
            } catch (e: Exception) {
                val fallbackName = currentUser.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }

                if (!fallbackName.isNullOrEmpty()) {
                    tvGreeting.text = "$greetingBase $fallbackName"
                }
            }
        }
    }

    /** Thiết lập RecyclerView lịch sử gần đây và nút expand. */
    private fun setupRecentHistory() {
        val homeRoot = binding.homeContainer.root
        val rvRecentHistory = homeRoot.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.rvRecentHistory
        ) ?: return
        val emptyRecentState = homeRoot.findViewById<View>(R.id.emptyRecentState)
        val recentHeader = homeRoot.findViewById<View>(R.id.recentHeader)
        val recentContainer = homeRoot.findViewById<net.cachapa.expandablelayout.ExpandableLayout>(
            R.id.recentContainer
        )
        val ivRecentExpandIcon = homeRoot.findViewById<ImageView>(R.id.ivRecentExpandIcon)

        recentHistoryAdapter = RecentHistoryAdapter { entry -> onNavigateToDetail(entry) }

        rvRecentHistory.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = recentHistoryAdapter
            isNestedScrollingEnabled = false
        }

        recentHeader?.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            toggleRecentExpansion(recentContainer, ivRecentExpandIcon)
        }

        loadRecentHistory(emptyRecentState)
    }

    /** Toggle expand/collapse section lịch sử gần đây. */
    private fun toggleRecentExpansion(
        container: net.cachapa.expandablelayout.ExpandableLayout?,
        expandIcon: ImageView?
    ) {
        container ?: return
        expandIcon ?: return

        if (container.isExpanded) {
            container.collapse()
            expandIcon.animate().rotation(0f).setDuration(300).start()
        } else {
            container.expand()
            expandIcon.animate().rotation(180f).setDuration(300).start()

            container.setOnExpansionUpdateListener { expansionFraction, _ ->
                if (expansionFraction == 1f) {
                    val homeScrollView = binding.homeContainer.root
                        .findViewById<androidx.core.widget.NestedScrollView>(R.id.homeScrollView)
                    val sectionRecent = binding.homeContainer.root
                        .findViewById<View>(R.id.sectionRecent)

                    homeScrollView?.smoothScrollTo(0, sectionRecent?.top ?: 0)
                    container.setOnExpansionUpdateListener(null)
                }
            }
        }
    }

    /** Tải và hiển thị tữ lịch sử, cập nhật empty state. */
    private fun loadRecentHistory(emptyState: View?) {
        activity.lifecycleScope.launch {
            val placeholders = List(3) { i ->
                HistoryEntry(
                    id = -1 - i,
                    timestamp = 0
                )
            }
            recentHistoryAdapter.submitList(placeholders)

            historyRepository.getAllHistoryNewestFirst().collect { allHistory ->
                val recentItems = allHistory.take(5)
                recentHistoryAdapter.submitList(recentItems)
                emptyState?.isVisible = recentItems.isEmpty()
            }
        }
    }

    /** Thiết lập RecyclerView explore ngang với placeholder loading. */
    private fun setupQuickExplore() {
        val homeRoot = binding.homeContainer.root
        val rvQuickExplore = homeRoot.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.rvQuickExplore
        )

        exploreAdapter = ExploreAdapter { item ->
            if (item.image.isNotEmpty()) {
                onExploreItemClick(item.image)
            }
        }

        rvQuickExplore.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = exploreAdapter
        }

        val placeholders = List(5) { i ->
            com.nguyendevs.ecolens.models.ExploreItem(
                id = "placeholder_$i",
                name = "Loading name.",
                desc = "Loading description."
            )
        }
        exploreAdapter.submitList(placeholders)
    }
}