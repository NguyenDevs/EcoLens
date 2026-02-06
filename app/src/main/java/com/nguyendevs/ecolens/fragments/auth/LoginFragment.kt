package com.nguyendevs.ecolens.fragments.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.FragmentLoginBinding
import com.nguyendevs.ecolens.handlers.auth.GoogleSignInHandler
import com.nguyendevs.ecolens.handlers.auth.LoginHandler
import com.nguyendevs.ecolens.handlers.auth.RegisterHandler
import com.nguyendevs.ecolens.managers.auth.AuthUIManager

/**
 * Fragment cho màn hình Login/Register
 * Hỗ trợ email/password và Google Sign-In
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: UserRepository
    private lateinit var authUIManager: AuthUIManager
    private lateinit var loginHandler: LoginHandler
    private lateinit var registerHandler: RegisterHandler
    private lateinit var googleSignInHandler: GoogleSignInHandler

    private var videoPrepared = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = UserRepository()
        authUIManager = AuthUIManager(binding, requireContext())
        loginHandler = LoginHandler(requireContext(), userRepository, lifecycleScope)
        registerHandler = RegisterHandler(requireContext(), userRepository, lifecycleScope)

        googleSignInHandler = GoogleSignInHandler(
            this,
            userRepository,
            lifecycleScope
        )
        binding.root.post {
            setupUI()
        }
    }


    // ==================== UI SETUP ====================

    private fun setupUI() {
        authUIManager.setupTabs()
        setupGoogleSignIn()
        setupAuthButton()
        setupForgotPassword()
    }

    private fun setupGoogleSignIn() {
        googleSignInHandler.setup(
            rememberMe = authUIManager.isRememberMeChecked(),
            onLoadingChange = { isLoading -> setLoading(isLoading) },
            onSuccess = { LoginHandler.navigateToMain(requireContext()) }
        )

        binding.btnGoogle.setOnClickListener {
            googleSignInHandler.signIn()
        }
    }

    private fun setupAuthButton() {
        binding.btnAuthAction.setOnClickListener {
            val email = authUIManager.getEmail()
            val password = authUIManager.getPassword()

            if (authUIManager.isLoginMode()) {
                // Login mode
                loginHandler.handleLogin(
                    email = email,
                    password = password,
                    rememberMe = authUIManager.isRememberMeChecked(),
                    onLoadingChange = { isLoading -> setLoading(isLoading) },
                    onSuccess = { LoginHandler.navigateToMain(requireContext()) }
                )
            } else {
                // Register mode
                registerHandler.handleRegister(
                    email = email,
                    password = password,
                    confirmPassword = authUIManager.getConfirmPassword(),
                    agreeTerms = authUIManager.isAgreeTermsChecked(),
                    onLoadingChange = { isLoading -> setLoading(isLoading) },
                    onSuccess = { LoginHandler.navigateToMain(requireContext()) }
                )
            }
        }
    }

    private fun setupForgotPassword() {
        binding.tvForgotPassword.setOnClickListener {
            (activity as? AuthActivity)?.navigateToForgotPassword()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        (activity as? AuthActivity)?.setFragmentLoading(isLoading)
        binding.btnAuthAction.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

}