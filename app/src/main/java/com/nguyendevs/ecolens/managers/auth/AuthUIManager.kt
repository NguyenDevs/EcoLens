package com.nguyendevs.ecolens.managers.auth

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding

/**
 * Manager cho UI logic của AuthActivity
 * Quản lý tabs, button states, visibility
 */
class AuthUIManager(
    private val binding: ActivityAuthBinding,
    private val context: Context
) {

    fun setupTabs() {
        updateTabTypeface(binding.tabLayoutAuth.selectedTabPosition)

        binding.tabLayoutAuth.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                updateTabTypeface(position)

                when (position) {
                    0 -> showLoginMode()
                    1 -> showRegisterMode()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showLoginMode() {
        binding.tilConfirmPassword.visibility = View.GONE
        binding.cbAgreeTerms.visibility = View.GONE
        (binding.cbRememberMe.parent as? View)?.visibility = View.VISIBLE
        binding.btnAuthAction.text = context.getString(R.string.login)
    }

    private fun showRegisterMode() {
        (binding.cbRememberMe.parent as? View)?.visibility = View.GONE
        binding.cbAgreeTerms.visibility = View.VISIBLE
        binding.tilConfirmPassword.visibility = View.VISIBLE
        binding.btnAuthAction.text = context.getString(R.string.register)
    }

    private fun updateTabTypeface(selectedPosition: Int) {
        val slidingTabStrip = binding.tabLayoutAuth.getChildAt(0) as? ViewGroup
            ?: return

        for (i in 0 until slidingTabStrip.childCount) {
            val tabView = slidingTabStrip.getChildAt(i) as? ViewGroup
            tabView?.let {
                for (j in 0 until it.childCount) {
                    val child = it.getChildAt(j)
                    if (child is TextView) {
                        child.typeface = if (i == selectedPosition) {
                            Typeface.DEFAULT_BOLD
                        } else {
                            Typeface.DEFAULT
                        }
                    }
                }
            }
        }
    }

    fun isLoginMode(): Boolean {
        return binding.tabLayoutAuth.selectedTabPosition == 0
    }

    fun getEmail(): String = binding.etEmail.text.toString().trim()
    fun getPassword(): String = binding.etPassword.text.toString().trim()
    fun getConfirmPassword(): String = binding.etConfirmPassword.text.toString().trim()
    fun isRememberMeChecked(): Boolean = binding.cbRememberMe.isChecked
    fun isAgreeTermsChecked(): Boolean = binding.cbAgreeTerms.isChecked
}