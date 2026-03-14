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
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.FragmentLoginBinding
import com.nguyendevs.ecolens.handlers.auth.GoogleSignInHandler
import com.nguyendevs.ecolens.handlers.auth.LoginHandler
import com.nguyendevs.ecolens.handlers.auth.RegisterHandler
import com.nguyendevs.ecolens.managers.auth.AuthUIManager
import com.nguyendevs.ecolens.managers.setting.LanguageManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var userRepository: UserRepository
    private lateinit var authUIManager: AuthUIManager
    private lateinit var loginHandler: LoginHandler
    private lateinit var registerHandler: RegisterHandler
    private lateinit var googleSignInHandler: GoogleSignInHandler

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
        val languageManager = LanguageManager(requireContext())
        authUIManager = AuthUIManager(binding, requireContext())
        loginHandler =
                LoginHandler(requireContext(), userRepository, lifecycleScope, languageManager)
        registerHandler =
                RegisterHandler(requireContext(), userRepository, lifecycleScope, languageManager)

        googleSignInHandler =
                GoogleSignInHandler(this, userRepository, lifecycleScope, languageManager)

        val isLogin = savedInstanceState?.getBoolean("is_login", true) ?: true
        authUIManager.setupLayout(isLogin)

        binding.root.post {
            setupPageListeners()
            setupGoogleSignIn()
            playEntranceAnimations()
        }
    }

    private fun setupPageListeners() {
        authUIManager.setForgotPasswordClickListener {
            (activity as? AuthActivity)?.navigateToForgotPassword()
        }

        authUIManager.setLoginButtonClickListener {
            val email = authUIManager.getEmail()
            val password = authUIManager.getPassword()
            loginHandler.handleLogin(
                    email = email,
                    password = password,
                    rememberMe = authUIManager.isRememberMeChecked(),
                    onLoadingChange = { isLoading -> setLoading(isLoading) },
                    onSuccess = { LoginHandler.navigateToMain(requireContext()) }
            )
        }

        authUIManager.setRegisterButtonClickListener {
            val email = authUIManager.getEmail()
            val password = authUIManager.getPassword()
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

    private fun playEntranceAnimations() {
        val logoAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.auth_logo_scale_in)
        val titleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in)
        val subtitleAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 100
                }
        val cardAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 200
                }
        val socialAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 350
                }
        val orAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.auth_slide_up_fade_in).apply {
                    startOffset = 300
                }

        binding.logoContainer.startAnimation(logoAnim)
        binding.logoGlow.startAnimation(logoAnim)
        binding.tvWelcome.startAnimation(titleAnim)
        binding.tvSubtitle.startAnimation(subtitleAnim)
        binding.cardForm.startAnimation(cardAnim)
        binding.layoutOr.startAnimation(orAnim)
        binding.layoutSocialButtons.startAnimation(socialAnim)
    }

    private fun setupGoogleSignIn() {
        googleSignInHandler.setup(
                rememberMe = authUIManager.isRememberMeChecked(),
                onLoadingChange = { isLoading -> setLoading(isLoading) },
                onSuccess = { LoginHandler.navigateToMain(requireContext()) }
        )

        binding.btnGoogle.setOnClickListener { googleSignInHandler.signIn() }
    }

    private fun setLoading(isLoading: Boolean) {
        (activity as? AuthActivity)?.setFragmentLoading(isLoading)
        binding.btnGoogle.isEnabled = !isLoading
        authUIManager.setLoadingState(isLoading)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::authUIManager.isInitialized) {
            outState.putBoolean("is_login", authUIManager.isLoginMode())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
