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

    private val firebaseAuth = FirebaseAuth.getInstance()

    fun toggleAccountDetails() {
        // Prevent toggling while a transition is occurring immediately
        if (isTransitioning()) return

        setTransitioning(true)

        if (binding.accountDetailsContainer.isExpanded) {
            collapseAccountDetails()
        } else {
            expandAccountDetails()
        }
    }

    private fun expandAccountDetails() {
        val user = firebaseAuth.currentUser
        val isGoogleUser =
                user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

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

            // Need to set alpha back to 1 if we're migrating from stagger animation
            binding.changePasswordOption.alpha = 1f
            binding.linkGoogleOption.alpha = 1f

            binding.changePasswordOption.visibility = View.VISIBLE
            binding.linkGoogleOption.visibility = View.VISIBLE
        }

        binding.changeUsernameOption.alpha = 1f
        binding.deleteAccountOption.alpha = 1f

        binding.changeUsernameOption.visibility = View.VISIBLE
        binding.deleteAccountOption.visibility = View.VISIBLE
        binding.dividerDeleteaccount.visibility = View.VISIBLE

        binding.accountDetailsContainer.expand()

        binding.ivExpandIcon
                .animate()
                .rotation(180f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { setTransitioning(false) }
                .start()
    }

    private fun collapseAccountDetails() {
        binding.accountDetailsContainer.collapse()

        binding.ivExpandIcon
                .animate()
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { setTransitioning(false) }
                .start()
    }
}
