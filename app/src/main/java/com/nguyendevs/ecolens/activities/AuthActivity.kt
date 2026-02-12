package com.nguyendevs.ecolens.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.auth.BaseAuthActivity
import com.nguyendevs.ecolens.fragments.auth.ForgotPasswordFragment
import com.nguyendevs.ecolens.fragments.auth.LoginFragment
import com.nguyendevs.ecolens.managers.auth.AutoLoginManager
import com.nguyendevs.ecolens.managers.setting.LanguageManager

class AuthActivity : BaseAuthActivity() {

    private lateinit var autoLoginManager: AutoLoginManager
    private lateinit var languageManager: LanguageManager

    private val languageCycle =
            listOf(
                    LanguageManager.LANG_VI,
                    LanguageManager.LANG_EN,
                    LanguageManager.LANG_CN,
                    LanguageManager.LANG_JP
            )

    private val languageLabels =
            mapOf(
                    LanguageManager.LANG_VI to "VI",
                    LanguageManager.LANG_EN to "EN",
                    LanguageManager.LANG_CN to "CN",
                    LanguageManager.LANG_JP to "JP"
            )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        languageManager = LanguageManager(this)
        autoLoginManager = AutoLoginManager(this, userRepository)

        if (autoLoginManager.checkAndAutoLogin()) {
            return
        }

        setupLanguageButton()

        if (savedInstanceState == null) {
            loadFragment(LoginFragment())
        }
    }

    private fun setupLanguageButton() {
        updateLanguageLabel()
        binding.btnLanguage.setOnClickListener { cycleLanguage() }
    }

    private fun updateLanguageLabel() {
        val currentLang = languageManager.getLanguage()
        binding.tvLanguageLabel.text = languageLabels[currentLang] ?: "VI"
    }

    private fun cycleLanguage() {
        val currentLang = languageManager.getLanguage()
        val currentIndex = languageCycle.indexOf(currentLang)
        val nextIndex = (currentIndex + 1) % languageCycle.size
        val nextLang = languageCycle[nextIndex]

        languageManager.setLanguage(nextLang)
        recreate()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    fun navigateToForgotPassword() {
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.auth_fade_slide_right,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.auth_fade_slide_left
                )
                .replace(R.id.fragmentContainer, ForgotPasswordFragment())
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
