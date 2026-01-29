package com.nguyendevs.ecolens.handlers.main

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.RecentHistoryAdapter
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.util.Calendar
import kotlinx.coroutines.launch

/**
 * Handler quản lý Home Screen components. Xử lý: greeting động, recent history, quick explore,
 * toggle recent section.
 */
class HomeScreenHandler(
        private val activity: AppCompatActivity,
        private val binding: ActivityMainBinding,
        private val onNavigateToDetail: (HistoryEntry) -> Unit
) {

    private lateinit var recentHistoryAdapter: RecentHistoryAdapter
    private val historyRepository by lazy {
        HistoryRepository(
                HistoryDatabase.getDatabase(activity.applicationContext).historyDao(),
                activity.applicationContext
        )
    }
    private var isRecentExpanded = true

    /** Setup tất cả components của Home Screen */
    fun setup() {
        setupGreeting()
        setupHeroCardButton()
        setupRecentHistory()
        setupQuickExplore()
    }

    /** Setup hero card button để trigger camera */
    private fun setupHeroCardButton() {
        val homeRoot = binding.homeContainer.root
        homeRoot.findViewById<View>(R.id.btnStartNow)?.setOnClickListener {
            binding.fabCamera.performClick()
        }
    }

    /** Setup greeting động theo thời gian trong ngày + Username từ Firebase */
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

        val greetingBase = activity.getString(greetingResId)
        tvGreeting.text = greetingBase

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            activity.lifecycleScope.launch {
                try {
                    val userDetails = UserRepository().getCurrentUserDetails()
                    val username =
                            userDetails?.username?.takeIf { it.isNotBlank() }
                                    ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                                            ?: currentUser.email?.substringBefore("@")?.takeIf {
                                        it.isNotBlank()
                                    }

                    if (!username.isNullOrEmpty()) {
                        tvGreeting.text = "$greetingBase $username"
                    }
                } catch (e: Exception) {
                    val fallbackName =
                            currentUser.displayName?.takeIf { it.isNotBlank() }
                                    ?: currentUser.email?.substringBefore("@")?.takeIf {
                                        it.isNotBlank()
                                    }
                    if (!fallbackName.isNullOrEmpty()) {
                        tvGreeting.text = "$greetingBase $fallbackName"
                    }
                }
            }
        }
    }

    /** Setup RecyclerView cho Recent History với 5 item gần nhất */
    private fun setupRecentHistory() {
        val homeRoot = binding.homeContainer.root
        val rvRecentHistory =
                homeRoot.findViewById<androidx.recyclerview.widget.RecyclerView>(
                        R.id.rvRecentHistory
                )
                        ?: return
        val emptyRecentState = homeRoot.findViewById<View>(R.id.emptyRecentState)
        val recentHeader = homeRoot.findViewById<View>(R.id.recentHeader)
        val recentContainer = homeRoot.findViewById<View>(R.id.recentContainer)
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

    /** Toggle expand/collapse cho Recent History section */
    private fun toggleRecentExpansion(container: View?, expandIcon: ImageView?) {
        container ?: return
        expandIcon ?: return

        if (isRecentExpanded) {
            isRecentExpanded = false
            expandIcon.animate().rotation(0f).setDuration(300).start()
            animateRecentHeight(container, container.height, 0) { container.visibility = View.GONE }
        } else {
            isRecentExpanded = true
            expandIcon.animate().rotation(180f).setDuration(300).start()
            container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val targetHeight = container.measuredHeight
            container.layoutParams.height = 0
            container.visibility = View.VISIBLE
            animateRecentHeight(container, 0, targetHeight)
        }
    }

    /** Animate height của Recent History container */
    private fun animateRecentHeight(view: View, from: Int, to: Int, onEnd: (() -> Unit)? = null) {
        val animator = android.animation.ValueAnimator.ofInt(from, to)
        animator.addUpdateListener { animation ->
            view.layoutParams.height = animation.animatedValue as Int
            view.requestLayout()
        }
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 300
        onEnd?.let {
            animator.addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            it()
                        }
                    }
            )
        }
        animator.start()
    }

    /** Load 5 item lịch sử gần nhất từ database */
    private fun loadRecentHistory(emptyState: View?) {
        activity.lifecycleScope.launch {
            historyRepository.getAllHistoryNewestFirst().collect { allHistory ->
                val recentItems = allHistory.take(5)
                recentHistoryAdapter.submitList(recentItems)
                emptyState?.isVisible = recentItems.isEmpty()
            }
        }
    }


    // TẠM THỜI HARDCODE
    /** Setup Quick Explore với dữ liệu hardcoded */
    private fun setupQuickExplore() {
        val homeRoot = binding.homeContainer.root

        val card1 = homeRoot.findViewById<View>(R.id.exploreCard1)
        card1?.findViewById<TextView>(R.id.tvExploreName)?.text =
                activity.getString(R.string.explore_item_1_name)
        card1?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                activity.getString(R.string.explore_item_1_desc)
        card1?.findViewById<ImageView>(R.id.imgExplore)?.setImageResource(R.drawable.succulent)

        val card2 = homeRoot.findViewById<View>(R.id.exploreCard2)
        card2?.findViewById<TextView>(R.id.tvExploreName)?.text =
                activity.getString(R.string.explore_item_2_name)
        card2?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                activity.getString(R.string.explore_item_2_desc)
        card2?.findViewById<ImageView>(R.id.imgExplore)
                ?.setImageResource(R.drawable.monarch_butterfly)

        val card3 = homeRoot.findViewById<View>(R.id.exploreCard3)
        card3?.findViewById<TextView>(R.id.tvExploreName)?.text =
                activity.getString(R.string.explore_item_3_name)
        card3?.findViewById<TextView>(R.id.tvExploreDesc)?.text =
                activity.getString(R.string.explore_item_3_desc)
        card3?.findViewById<ImageView>(R.id.imgExplore)?.setImageResource(R.drawable.lavender)
    }
}
