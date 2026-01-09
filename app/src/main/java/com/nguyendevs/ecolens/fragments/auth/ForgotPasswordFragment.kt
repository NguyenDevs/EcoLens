package com.nguyendevs.ecolens.fragments.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.handlers.auth.ForgotPasswordHandler
import com.nguyendevs.ecolens.databinding.FragmentForgotPasswordBinding

/**
 * Fragment cho màn hình Forgot Password
 * Cho phép người dùng nhập email để nhận link reset password
 */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var forgotPasswordHandler: ForgotPasswordHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize handler
        forgotPasswordHandler = ForgotPasswordHandler(requireContext(), lifecycleScope)

        setupUI()
    }

    // ==================== UI SETUP ====================

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            (activity as? AuthActivity)?.navigateBackToLogin()
        }

        // Back to login text
        binding.tvBackToLogin.setOnClickListener {
            (activity as? AuthActivity)?.navigateBackToLogin()
        }

        // Send reset link button
        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            forgotPasswordHandler.sendPasswordResetEmail(
                email = email,
                onLoadingChange = { isLoading -> setLoading(isLoading) },
                onSuccess = {
                    // Delay để user đọc message, sau đó quay lại login
                    binding.root.postDelayed({
                        (activity as? AuthActivity)?.navigateBackToLogin()
                    }, 2000)
                }
            )
        }

        // Enter key trên keyboard
        binding.etEmail.setOnEditorActionListener { _, _, _ ->
            binding.btnSendResetLink.performClick()
            true
        }
    }

    /**
     * Bật/tắt trạng thái loading
     */
    private fun setLoading(isLoading: Boolean) {
        (activity as? AuthActivity)?.setFragmentLoading(isLoading)
        binding.btnSendResetLink.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }

    // ==================== LIFECYCLE ====================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}