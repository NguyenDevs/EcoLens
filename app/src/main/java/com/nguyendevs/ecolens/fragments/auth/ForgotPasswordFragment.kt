package com.nguyendevs.ecolens.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.databinding.FragmentForgotPasswordBinding
import com.nguyendevs.ecolens.handlers.auth.ForgotPasswordHandler

/** Fragment màn hình quên mật khẩu, gửi email đặt lại mật khẩu. */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var forgotPasswordHandler: ForgotPasswordHandler

    /** Inflate layout của fragment. */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Khởi tạo handler và thiết lập UI. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        forgotPasswordHandler = ForgotPasswordHandler(requireContext(), lifecycleScope)

        setupUI()
        playEntranceAnimations()
    }

    /** Thiết lập các listener cho nút back và gửi link đặt lại. */
    private fun setupUI() {
        binding.btnBack.setOnClickListener { (activity as? AuthActivity)?.navigateBackToLogin() }

        binding.tvBackToLogin.setOnClickListener {
            (activity as? AuthActivity)?.navigateBackToLogin()
        }

        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            forgotPasswordHandler.sendPasswordResetEmail(
                    email = email,
                    onLoadingChange = { isLoading -> setLoading(isLoading) },
                    onSuccess = {
                        binding.root.postDelayed(
                                { (activity as? AuthActivity)?.navigateBackToLogin() },
                                2000
                        )
                    }
            )
        }

        binding.etEmail.setOnEditorActionListener { _, _, _ ->
            binding.btnSendResetLink.performClick()
            true
        }
    }

    /** Chạy các animation xuất hiện khi mở fragment. */
    private fun playEntranceAnimations() {
        val logoAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.auth_logo_scale_in)
        val titleAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 100
                }
        val descAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 200
                }
        val cardAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 300
                }
        val backLinkAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 400
                }

        binding.cardIcon.startAnimation(logoAnim)
        binding.tvTitle.startAnimation(titleAnim)
        binding.tvDescription.startAnimation(descAnim)
        binding.cardEmailInput.startAnimation(cardAnim)
        binding.layoutBackToLogin.startAnimation(backLinkAnim)
    }

    /** Đặt trạng thái loading, vô hiệu hóa các nút khi đang xử lý. */
    private fun setLoading(isLoading: Boolean) {
        (activity as? AuthActivity)?.setFragmentLoading(isLoading)
        binding.btnSendResetLink.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
