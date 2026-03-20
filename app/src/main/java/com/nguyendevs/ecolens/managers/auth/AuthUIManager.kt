package com.nguyendevs.ecolens.managers.auth

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.FragmentLoginBinding

/** Manager quản lý chuyển đổi giao diện màn hình đăng nhập và đăng ký. */
class AuthUIManager(private val binding: FragmentLoginBinding, private val context: Context) {

    private val tabTitles = arrayOf(R.string.login, R.string.register)
    private var isLoginMode = true

    /** Cấu hình bố cục mặc định khi mở màn hình. */
    fun setupLayout(initialIsLogin: Boolean = true) {
        setupTabs()
        this.isLoginMode = initialIsLogin
        val initialTabIndex = if (initialIsLogin) 0 else 1
        
        binding.tabLayoutAuth.getTabAt(initialTabIndex)?.select()
        updateTabTypeface(initialTabIndex)
        
        if (initialIsLogin) {
            binding.expandableLogin.setExpanded(true, false)
            binding.expandableRegister.setExpanded(false, false)
            binding.layoutLoginFields.alpha = 1f
            binding.layoutRegisterFields.alpha = 0f
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnLogin.alpha = 1f
            binding.btnRegister.visibility = View.GONE
            binding.btnRegister.alpha = 0f
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        } else {
            binding.expandableLogin.setExpanded(false, false)
            binding.expandableRegister.setExpanded(true, false)
            binding.layoutLoginFields.alpha = 0f
            binding.layoutRegisterFields.alpha = 1f
            binding.btnLogin.visibility = View.GONE
            binding.btnLogin.alpha = 0f
            binding.btnRegister.visibility = View.VISIBLE
            binding.btnRegister.alpha = 1f
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
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

    /** Kích hoạt đóng/mở giao diện theo trạng thái form. */
    private fun toggleMode(isLogin: Boolean) {
        if (this.isLoginMode == isLogin) return
        this.isLoginMode = isLogin

        val duration = 400L

        if (isLogin) {
            binding.expandableLogin.expand()
            binding.expandableRegister.collapse()
            
            binding.layoutLoginFields.animate().alpha(1f).setDuration(duration).start()
            binding.layoutRegisterFields.animate().alpha(0f).setDuration(duration).start()
            
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnLogin.alpha = 1f
            binding.btnRegister.visibility = View.GONE
            binding.btnRegister.alpha = 0f
            
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        } else {
            binding.expandableLogin.collapse()
            binding.expandableRegister.expand()
            
            binding.layoutLoginFields.animate().alpha(0f).setDuration(duration).start()
            binding.layoutRegisterFields.animate().alpha(1f).setDuration(duration).start()
            
            binding.btnRegister.visibility = View.VISIBLE
            binding.btnRegister.alpha = 1f
            binding.btnLogin.visibility = View.GONE
            binding.btnLogin.alpha = 0f
            
            binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
        
        binding.etEmail.requestFocus()
        val text = binding.etEmail.text
        if (text != null) {
            binding.etEmail.setSelection(text.length)
        }
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
