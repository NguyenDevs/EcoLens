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
import com.nguyendevs.ecolens.model.LoadingStage

/**
 * Handler quản lý Bottom Navigation và trạng thái các containers. Xử lý: tab switching, container
 * visibility, save/restore last tab.
 */
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

    /** Setup bottom navigation listener */
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

    /** Navigate to specific tab */
    fun navigateTo(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    /** Get current selected tab */
    fun getCurrentTab(): Int = binding.bottomNavigation.selectedItemId

    /** Restore last selected tab */
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
            checkSpeaking: Boolean = true,
            uiStateChecker: (() -> Triple<LoadingStage, Boolean, Boolean>)? = null
    ) {
        onTabChanged(itemId)

        val transition = Fade()
        transition.duration = TRANSITION_DURATION_MS
        TransitionManager.beginDelayedTransition(binding.mainContent, transition)

        // Hide all containers
        binding.homeContainer.root.visibility = View.GONE
        binding.historyContainer.visibility = View.GONE
        binding.myGardenContainer.visibility = View.GONE
        binding.settingsContainer.root.visibility = View.GONE
        binding.searchBarContainer.visibility = View.GONE
        binding.fabSpeak.visibility = View.GONE
        binding.fabMute.visibility = View.GONE

        // Show bottom nav and camera FAB
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fabCamera.visibility = View.VISIBLE

        when (itemId) {
            R.id.nav_home -> {
                binding.homeContainer.root.visibility = View.VISIBLE
                binding.searchBarContainer.visibility = View.VISIBLE

                // Check UI state for speaker FAB visibility
                uiStateChecker?.let { checker ->
                    val (loadingStage, isSpeaking, hasInfo) = checker()
                    val isComplete = loadingStage == LoadingStage.COMPLETE

                    if (isComplete && hasInfo && !isSpeaking) {
                        binding.fabSpeak.visibility = View.VISIBLE
                    } else if (isSpeaking) {
                        binding.fabMute.visibility = View.VISIBLE
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
    }

    /** Check if current tab is Home */
    fun isHomeTab(): Boolean = binding.bottomNavigation.selectedItemId == R.id.nav_home
}
