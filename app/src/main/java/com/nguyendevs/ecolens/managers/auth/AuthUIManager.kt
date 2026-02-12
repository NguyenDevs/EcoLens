package com.nguyendevs.ecolens.managers.auth

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.auth.AuthPagerAdapter
import com.nguyendevs.ecolens.databinding.FragmentLoginBinding
import kotlin.math.abs

class AuthUIManager(private val binding: FragmentLoginBinding, private val context: Context) {

    private val pagerAdapter = AuthPagerAdapter()
    private val tabTitles = arrayOf(R.string.login, R.string.register)
    private var isAnimating = false

    fun setupViewPager() {
        binding.viewPagerAuth.adapter = pagerAdapter
        binding.viewPagerAuth.offscreenPageLimit = 1
        binding.viewPagerAuth.isUserInputEnabled = false

        AuthPagerAdapter.setupDynamicHeight(binding.viewPagerAuth)

        binding.viewPagerAuth.setPageTransformer { page, position ->
            val absPos = abs(position)
            page.alpha = 1f - absPos * 0.5f
            page.scaleX = 1f - absPos * 0.08f
            page.scaleY = 1f - absPos * 0.08f
        }

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
                        val targetPosition = tab?.position ?: 0
                        if (binding.viewPagerAuth.currentItem != targetPosition && !isAnimating) {
                            smoothScrollToPage(targetPosition)
                        }
                        updateTabTypeface(targetPosition)
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                }
        )
    }

    private fun smoothScrollToPage(targetPosition: Int) {
        val currentPosition = binding.viewPagerAuth.currentItem
        if (currentPosition == targetPosition || isAnimating) return

        isAnimating = true
        val pageWidth = binding.viewPagerAuth.width.toFloat()
        val direction = if (targetPosition > currentPosition) -1f else 1f

        binding.viewPagerAuth.beginFakeDrag()

        val animator = ValueAnimator.ofFloat(0f, pageWidth)
        animator.duration = 400
        animator.interpolator = DecelerateInterpolator(2f)

        var previousValue = 0f
        animator.addUpdateListener { anim ->
            val currentValue = anim.animatedValue as Float
            val delta = currentValue - previousValue
            previousValue = currentValue
            if (binding.viewPagerAuth.isFakeDragging) {
                binding.viewPagerAuth.fakeDragBy(delta * direction)
            }
        }

        animator.addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (binding.viewPagerAuth.isFakeDragging) {
                            binding.viewPagerAuth.endFakeDrag()
                        }
                        isAnimating = false
                    }

                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        if (binding.viewPagerAuth.isFakeDragging) {
                            binding.viewPagerAuth.endFakeDrag()
                        }
                        isAnimating = false
                    }
                }
        )

        animator.start()
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

    fun isLoginMode(): Boolean = binding.viewPagerAuth.currentItem == 0

    fun getEmail(): String {
        val page =
                if (isLoginMode()) pagerAdapter.getLoginPage() else pagerAdapter.getRegisterPage()
        return page?.findViewById<TextInputEditText>(R.id.etEmail)?.text?.toString()?.trim() ?: ""
    }

    fun getPassword(): String {
        val page =
                if (isLoginMode()) pagerAdapter.getLoginPage() else pagerAdapter.getRegisterPage()
        return page?.findViewById<TextInputEditText>(R.id.etPassword)?.text?.toString()?.trim()
                ?: ""
    }

    fun getConfirmPassword(): String {
        return pagerAdapter
                .getRegisterPage()
                ?.findViewById<TextInputEditText>(R.id.etConfirmPassword)
                ?.text
                ?.toString()
                ?.trim()
                ?: ""
    }

    fun isRememberMeChecked(): Boolean {
        return pagerAdapter
                .getLoginPage()
                ?.findViewById<MaterialCheckBox>(R.id.cbRememberMe)
                ?.isChecked
                ?: false
    }

    fun isAgreeTermsChecked(): Boolean {
        return pagerAdapter
                .getRegisterPage()
                ?.findViewById<MaterialCheckBox>(R.id.cbAgreeTerms)
                ?.isChecked
                ?: false
    }

    fun setForgotPasswordClickListener(listener: View.OnClickListener) {
        pagerAdapter
                .getLoginPage()
                ?.findViewById<TextView>(R.id.tvForgotPassword)
                ?.setOnClickListener(listener)
    }

    fun setLoginButtonClickListener(listener: View.OnClickListener) {
        pagerAdapter
                .getLoginPage()
                ?.findViewById<MaterialButton>(R.id.btnLogin)
                ?.setOnClickListener(listener)
    }

    fun setRegisterButtonClickListener(listener: View.OnClickListener) {
        pagerAdapter
                .getRegisterPage()
                ?.findViewById<MaterialButton>(R.id.btnRegister)
                ?.setOnClickListener(listener)
    }

    fun setLoadingState(isLoading: Boolean) {
        pagerAdapter.getLoginPage()?.let { page ->
            page.findViewById<MaterialButton>(R.id.btnLogin)?.isEnabled = !isLoading
            page.findViewById<TextInputEditText>(R.id.etEmail)?.isEnabled = !isLoading
            page.findViewById<TextInputEditText>(R.id.etPassword)?.isEnabled = !isLoading
        }
        pagerAdapter.getRegisterPage()?.let { page ->
            page.findViewById<MaterialButton>(R.id.btnRegister)?.isEnabled = !isLoading
            page.findViewById<TextInputEditText>(R.id.etEmail)?.isEnabled = !isLoading
            page.findViewById<TextInputEditText>(R.id.etPassword)?.isEnabled = !isLoading
            page.findViewById<TextInputEditText>(R.id.etConfirmPassword)?.isEnabled = !isLoading
        }
    }

    fun getPagerAdapter(): AuthPagerAdapter = pagerAdapter
}
