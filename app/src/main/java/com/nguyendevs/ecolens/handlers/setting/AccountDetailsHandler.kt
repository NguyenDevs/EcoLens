package com.nguyendevs.ecolens.handlers.setting

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    private val firebaseAuth = FirebaseAuth.getInstance()

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

    private fun expandAccountDetails() {
        val user = firebaseAuth.currentUser
        val isGoogleUser = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

        binding.dividerUsername.visibility = View.VISIBLE
        
        // Hide Change Password and Link Google if user is logged in with Google
        if (isGoogleUser) {
            binding.dividerChangepassword.visibility = View.GONE
            binding.dividerLinkgoogle.visibility = View.GONE
            binding.changePasswordOption.visibility = View.GONE
            binding.linkGoogleOption.visibility = View.GONE
        } else {
            binding.dividerChangepassword.visibility = View.VISIBLE
            binding.dividerLinkgoogle.visibility = View.VISIBLE
        }
        
        binding.dividerDeleteaccount.visibility = View.VISIBLE

        binding.accountDetailsContainer.visibility = View.VISIBLE
        binding.accountDetailsContainer.alpha = 1f

        binding.ivExpandIcon
                .animate()
                .rotation(180f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

        val options = mutableListOf<View>()
        options.add(binding.changeUsernameOption)
        
        if (!isGoogleUser) {
            options.add(binding.changePasswordOption)
            options.add(binding.linkGoogleOption)
        }
        
        options.add(binding.deleteAccountOption)

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