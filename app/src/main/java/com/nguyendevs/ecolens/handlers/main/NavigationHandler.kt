package com.nguyendevs.ecolens.handlers.main

import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.fragments.chat.ChatHistoryFragment
import com.nguyendevs.ecolens.fragments.history.HistoryFragment
import com.nguyendevs.ecolens.models.LoadingStage

/** Handler quản lý Bottom Navigation và hiển thị các container tương ứng. */
class NavigationHandler(
        private val activity: AppCompatActivity,
        private val binding: ActivityMainBinding,
        private val sharedPreferences: SharedPreferences,
        private val historyFragment: HistoryFragment,
        private val chatHistoryFragment: ChatHistoryFragment,
        private val onTabChanged: (Int) -> Unit
) {

    companion object {
        const val KEY_LAST_NAV_ITEM = "last_nav_item"
        private const val TRANSITION_DURATION_MS = 120L
    }

    /** Thiết lập listener cho Bottom Navigation, lưu lại tab được chọn. */
    fun setup() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.bottomNavigation.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            if (activity.supportFragmentManager.backStackEntryCount > 0) {
                activity.supportFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }

            sharedPreferences.edit().putInt(KEY_LAST_NAV_ITEM, item.itemId).apply()

            updateNavigationState(item.itemId)
            true
        }
    }

    /** Điều hướng đến tab cụ thể. */
    fun navigateTo(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    /** Lấy tab đang được chọn hiện tại. */
    fun getCurrentTab(): Int = binding.bottomNavigation.selectedItemId

    /** Khôi phục tab đã chọn cuối cùng từ SharedPreferences. */
    fun restoreLastTab(navigateToSettings: Boolean = false): Int {
        return if (navigateToSettings) {
            R.id.nav_settings
        } else {
            sharedPreferences.getInt(KEY_LAST_NAV_ITEM, R.id.nav_home)
        }
    }

    /** Cập nhật trạng thái navigation dựa trên tab được chọn */
    fun updateNavigationState(
        itemId: Int,
        uiStateChecker: (() -> Triple<LoadingStage, Boolean, Boolean>)? = null
    ) {
        val currentTab = binding.bottomNavigation.selectedItemId
        if (currentTab == itemId && uiStateChecker == null && binding.homeContainer.root.visibility == View.VISIBLE) {
            return
        }

        val transition = Fade()
        transition.duration = TRANSITION_DURATION_MS
        TransitionManager.beginDelayedTransition(binding.mainContent, transition)

        binding.homeContainer.root.visibility = View.GONE
        binding.historyContainer.visibility = View.GONE
        binding.myGardenContainer.visibility = View.GONE
        binding.settingsContainer.root.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.fabSpeak.visibility = View.GONE

        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fabCamera.visibility = View.VISIBLE

        when (itemId) {
            R.id.nav_home -> {
                binding.homeContainer.root.visibility = View.VISIBLE
                binding.searchBarContainer.visibility = View.VISIBLE

                uiStateChecker?.let { checker ->
                    val (loadingStage, _, hasInfo) = checker()
                    val isComplete = loadingStage == LoadingStage.COMPLETE

                if (isComplete && hasInfo) {
                    binding.fabSpeak.visibility = View.VISIBLE
                } else {
                    binding.fabSpeak.visibility = View.GONE
                }
                }
            }
            R.id.nav_history -> {
                binding.historyContainer.visibility = View.VISIBLE
                if (!historyFragment.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(R.id.historyContainer, historyFragment, "HISTORY")
                            .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_my_garden -> {
                binding.myGardenContainer.visibility = View.VISIBLE
                if (!chatHistoryFragment.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(R.id.myGardenContainer, chatHistoryFragment, "CHAT_HISTORY")
                            .commitNowAllowingStateLoss()
                }
            }
            R.id.nav_settings -> {
                binding.settingsContainer.root.visibility = View.VISIBLE
            }
        }

        onTabChanged(itemId)
    }

    /** Kiểm tra tab hiện tại có phải Home không. */
    fun isHomeTab(): Boolean = binding.bottomNavigation.selectedItemId == R.id.nav_home
}