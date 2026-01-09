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

        // Initialize repository and managers
        userRepository = UserRepository()
        authUIManager = AuthUIManager(binding, requireContext())
        loginHandler = LoginHandler(requireContext(), userRepository, lifecycleScope)
        registerHandler = RegisterHandler(requireContext(), userRepository, lifecycleScope)

        // Cast requireActivity() to AppCompatActivity
        googleSignInHandler = GoogleSignInHandler(
            this,
            userRepository,
            lifecycleScope
        )

        // Setup video logo first
        setupVideoLogo()

        // Setup UI
        binding.root.post {
            setupUI()
        }
    }

    // ==================== VIDEO LOGO ====================

    private fun setupVideoLogo() {
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.auth_logo}")

        binding.videoLogo.apply {
            alpha = 0f
            visibility = View.VISIBLE
            videoPrepared = false

            setVideoURI(videoUri)

            setOnPreparedListener { mediaPlayer ->
                try {
                    videoPrepared = true
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)

                    binding.ivLogo.visibility = View.GONE

                    animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start()

                    start()
                } catch (e: Exception) {
                    showFallbackImage()
                }
            }

            setOnErrorListener { _, what, extra ->
                android.util.Log.e("LoginFragment", "Video error: $what / $extra")
                showFallbackImage()
                true
            }
        }

        binding.videoLogo.postDelayed({
            if (!videoPrepared) {
                android.util.Log.w("LoginFragment", "Video timeout → fallback")
                showFallbackImage()
            }
        }, 2000)
    }

    private fun showFallbackImage() {
        binding.videoLogo.apply {
            try {
                stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            visibility = View.GONE
        }

        binding.ivLogo.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(500)
                .start()
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

    // ==================== LIFECYCLE ====================

    override fun onPause() {
        super.onPause()
        if (_binding != null &&
            binding.videoLogo.visibility == View.VISIBLE &&
            binding.videoLogo.isPlaying) {
            binding.videoLogo.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null &&
            videoPrepared &&
            binding.videoLogo.visibility == View.VISIBLE &&
            !binding.videoLogo.isPlaying) {
            binding.videoLogo.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (_binding != null) {
            binding.videoLogo.removeCallbacks(null)
            if (binding.videoLogo.visibility == View.VISIBLE) {
                binding.videoLogo.stopPlayback()
            }
        }
        _binding = null
    }
}