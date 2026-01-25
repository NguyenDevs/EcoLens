package com.nguyendevs.ecolens.handlers.settings

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding

/**
 * Handler quản lý expand/collapse của account details section. Xử lý: toggle visibility, staggered
 * animations cho các option items.
 */
class AccountDetailsHandler(
        private val binding: ScreenSettingsBinding,
        private val isTransitioning: () -> Boolean,
        private val setTransitioning: (Boolean) -> Unit
) {

    private var isAccountDetailsExpanded = false

    /** Toggle expand/collapse account details với smooth animation và staggered delay */
    fun toggleAccountDetails() {
        if (isTransitioning()) return

        isAccountDetailsExpanded = !isAccountDetailsExpanded
        setTransitioning(true)

        if (isAccountDetailsExpanded) {
            expandAccountDetails()
        } else {
            collapseAccountDetails()
        }
    }

    /** Expand animation với staggered delay cho mỗi item */
    private fun expandAccountDetails() {
        binding.dividerUsername.visibility = View.VISIBLE
        binding.dividerChangepassword.visibility = View.VISIBLE
        binding.dividerLinkgoogle.visibility = View.VISIBLE
        binding.dividerDeleteaccount.visibility = View.VISIBLE

        // Show container
        binding.accountDetailsContainer.visibility = View.VISIBLE
        binding.accountDetailsContainer.alpha = 1f

        // Rotate chevron
        binding.ivExpandIcon
                .animate()
                .rotation(180f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

        val options =
                listOf(
                        binding.changeUsernameOption,
                        binding.changePasswordOption,
                        binding.linkGoogleOption,
                        binding.deleteAccountOption
                )

        options.forEachIndexed { index, option ->
            option.alpha = 0f
            option.translationY = -20f
            option.visibility = View.VISIBLE

            option.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((index * 50L))
                    .setDuration(250)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (index == options.size - 1) {
                            setTransitioning(false)
                        }
                    }
                    .start()
        }
    }

    /** Collapse animation - thu ngược lên cùng lúc không có delay */
    private fun collapseAccountDetails() {
        binding.accountDetailsContainer
                .animate()
                .alpha(0f)
                .translationY(-30f)
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    binding.dividerDeleteaccount.visibility = View.GONE
                    binding.dividerLinkgoogle.visibility = View.GONE
                    binding.dividerChangepassword.visibility = View.GONE
                    binding.dividerUsername.visibility = View.GONE
                    // Hide container
                    binding.accountDetailsContainer.visibility = View.GONE
                    binding.accountDetailsContainer.translationY = 0f

                    binding.changeUsernameOption.visibility = View.GONE
                    binding.changePasswordOption.visibility = View.GONE
                    binding.linkGoogleOption.visibility = View.GONE
                    binding.deleteAccountOption.visibility = View.GONE

                    setTransitioning(false)
                }
                .start()

        binding.ivExpandIcon
                .animate()
                .rotation(0f)
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
    }
}
