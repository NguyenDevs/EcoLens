package com.nguyendevs.ecolens.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.auth.BaseAuthActivity
import com.nguyendevs.ecolens.fragments.auth.ForgotPasswordFragment
import com.nguyendevs.ecolens.fragments.auth.LoginFragment
import com.nguyendevs.ecolens.managers.auth.AutoLoginManager

/**
 * Activity container cho authentication flow
 * Quản lý LoginFragment và ForgotPasswordFragment
 *
 * - BaseAuthActivity: Xử lý theme, loading
 * - AutoLoginManager: Quản lý auto login
 * - LoginFragment: UI cho login/register
 * - ForgotPasswordFragment: UI cho forgot password
 */
class AuthActivity : BaseAuthActivity() {

    private lateinit var autoLoginManager: AutoLoginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check auto login first
        autoLoginManager = AutoLoginManager(this, userRepository)
        if (autoLoginManager.checkAndAutoLogin()) {
            return
        }

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(LoginFragment())
        }
    }

    /**
     * Load fragment vào container
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Navigate to ForgotPasswordFragment with animation
     */
    fun navigateToForgotPassword() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.forgotpass_slide_right,
                0,
                0,
                R.anim.fade_out
            )
            .add(R.id.fragmentContainer, ForgotPasswordFragment())
            .addToBackStack(null)
            .commit()
    }

    /**
     * Navigate back to LoginFragment
     */
    fun navigateBackToLogin() {
        supportFragmentManager.popBackStack()
    }

    /**
     * Set loading state - called from fragments
     */
    fun setFragmentLoading(isLoading: Boolean) {
        setLoading(isLoading)
    }

    /**
     * Handle back press
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}