package com.nguyendevs.ecolens.handlers.setting

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding

/** Handler quản lý mở/đóng chi tiết tài khoản với hiệu ứng animation. */
class AccountDetailsHandler(
        private val binding: ScreenSettingsBinding,
        private val isTransitioning: () -> Boolean,
        private val setTransitioning: (Boolean) -> Unit
) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    /** Mở hoặc đóng section chi tiết tài khoản. */
    fun toggleAccountDetails() {
        if (isTransitioning()) return

        setTransitioning(true)

        if (binding.accountDetailsContainer.isExpanded) {
            collapseAccountDetails()
        } else {
            expandAccountDetails()
        }
    }

    /** Animation mở rộng giao diện và hiển thị thông tin. */
    private fun expandAccountDetails() {
        val user = firebaseAuth.currentUser
        val isGoogleUser =
                user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

        binding.dividerUsername.visibility = View.VISIBLE

        if (isGoogleUser) {
            binding.dividerChangepassword.visibility = View.GONE
            binding.dividerLinkgoogle.visibility = View.GONE
            binding.changePasswordOption.visibility = View.GONE
            binding.linkGoogleOption.visibility = View.GONE
        } else {
            binding.dividerChangepassword.visibility = View.VISIBLE
            binding.dividerLinkgoogle.visibility = View.VISIBLE

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

    /** Animation thu gọn giao diện chi tiết tài khoản. */
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
