package com.nguyendevs.ecolens.managers.auth

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.FragmentLoginBinding

class AuthUIManager(private val binding: FragmentLoginBinding, private val context: Context) {

    private val tabTitles = arrayOf(R.string.login, R.string.register)
    private var isLoginMode = true

    fun setupLayout() {
        setupTabs()
        updateTabTypeface(0)
    }

    private fun setupTabs() {
        val tab0 = binding.tabLayoutAuth.newTab().setText(context.getString(tabTitles[0]))
        val tab1 = binding.tabLayoutAuth.newTab().setText(context.getString(tabTitles[1]))
        binding.tabLayoutAuth.addTab(tab0)
        binding.tabLayoutAuth.addTab(tab1)

        binding.tabLayoutAuth.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val isLogin = tab?.position == 0
                        toggleMode(isLogin)
                        updateTabTypeface(tab?.position ?: 0)
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                }
        )
    }

    private fun toggleMode(isLogin: Boolean) {
        if (this.isLoginMode == isLogin) return
        this.isLoginMode = isLogin

        if (isLogin) {
            binding.expandableLogin.expand()
            binding.expandableRegister.collapse()
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        } else {
            binding.expandableLogin.collapse()
            binding.expandableRegister.expand()
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
        
        // Refresh focused view to update keyboard action if needed
        binding.etPassword.clearFocus()
        binding.etPassword.requestFocus()
    }

    private fun updateTabTypeface(selectedPosition: Int) {
        val slidingTabStrip = binding.tabLayoutAuth.getChildAt(0) as? ViewGroup ?: return

        for (i in 0 until slidingTabStrip.childCount) {
            val tabView = slidingTabStrip.getChildAt(i) as? ViewGroup
            tabView?.let {
                for (j in 0 until it.childCount) {
                    val child = it.getChildAt(j)
                    if (child is TextView) {
                        child.typeface =
                                if (i == selectedPosition) {
                                    Typeface.DEFAULT_BOLD
                                } else {
                                    Typeface.DEFAULT
                                }
                    }
                }
            }
        }
    }

    fun isLoginMode(): Boolean = isLoginMode

    fun getEmail(): String = binding.etEmail.text?.toString()?.trim() ?: ""

    fun getPassword(): String = binding.etPassword.text?.toString()?.trim() ?: ""

    fun getConfirmPassword(): String = binding.etConfirmPassword.text?.toString()?.trim() ?: ""

    fun isRememberMeChecked(): Boolean = binding.cbRememberMe.isChecked

    fun isAgreeTermsChecked(): Boolean = binding.cbAgreeTerms.isChecked

    fun setForgotPasswordClickListener(listener: View.OnClickListener) {
        binding.tvForgotPassword.setOnClickListener(listener)
    }

    fun setLoginButtonClickListener(listener: View.OnClickListener) {
        binding.btnLogin.setOnClickListener(listener)
    }

    fun setRegisterButtonClickListener(listener: View.OnClickListener) {
        binding.btnRegister.setOnClickListener(listener)
    }

    fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnRegister.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.cbRememberMe.isEnabled = !isLoading
        binding.cbAgreeTerms.isEnabled = !isLoading
    }
}
