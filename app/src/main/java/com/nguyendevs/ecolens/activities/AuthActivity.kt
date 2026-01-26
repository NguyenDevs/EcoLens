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
        autoLoginManager = AutoLoginManager(this, userRepository)
        if (autoLoginManager.checkAndAutoLogin()) {
            return
        }
        if (savedInstanceState == null) {
            loadFragment(LoginFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

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

    fun navigateBackToLogin() {
        supportFragmentManager.popBackStack()
    }

    fun setFragmentLoading(isLoading: Boolean) {
        setLoading(isLoading)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}