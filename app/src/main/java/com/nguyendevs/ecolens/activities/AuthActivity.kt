package com.nguyendevs.ecolens.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.activities.auth.*
import com.nguyendevs.ecolens.handlers.auth.GoogleSignInHandler
import com.nguyendevs.ecolens.handlers.auth.LoginHandler
import com.nguyendevs.ecolens.handlers.auth.RegisterHandler
import com.nguyendevs.ecolens.managers.auth.AuthUIManager
import com.nguyendevs.ecolens.managers.auth.AutoLoginManager

/**
 * Activity xử lý đăng nhập và đăng ký người dùng
 * Hỗ trợ email/password và Google Sign-In
 *
 * - BaseAuthActivity: Xử lý video logo, theme, loading
 * - AutoLoginManager: Quản lý auto login
 * - AuthUIManager: Quản lý UI (tabs, fields)
 * - LoginHandler: Xử lý login logic
 * - RegisterHandler: Xử lý register logic
 * - GoogleSignInHandler: Xử lý Google Sign-In
 */
class AuthActivity : BaseAuthActivity() {

    private lateinit var autoLoginManager: AutoLoginManager
    private lateinit var authUIManager: AuthUIManager
    private lateinit var loginHandler: LoginHandler
    private lateinit var registerHandler: RegisterHandler
    private lateinit var googleSignInHandler: GoogleSignInHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check auto login first
        autoLoginManager = AutoLoginManager(this, userRepository)
        if (autoLoginManager.checkAndAutoLogin()) {
            return
        }

        // Initialize managers
        authUIManager = AuthUIManager(binding, this)
        loginHandler = LoginHandler(this, userRepository, lifecycleScope)
        registerHandler = RegisterHandler(this, userRepository, lifecycleScope)
        googleSignInHandler = GoogleSignInHandler(this, userRepository, lifecycleScope)

        // Setup UI after layout is ready
        binding.root.post {
            setupUI()
        }
    }

    private fun setupUI() {
        authUIManager.setupTabs()
        setupGoogleSignIn()
        setupAuthButton()
    }

    private fun setupGoogleSignIn() {
        googleSignInHandler.setup(
            rememberMe = authUIManager.isRememberMeChecked(),
            onLoadingChange = { isLoading -> setLoading(isLoading) },
            onSuccess = { LoginHandler.navigateToMain(this) }
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
                    onSuccess = { LoginHandler.navigateToMain(this) }
                )
            } else {
                // Register mode
                registerHandler.handleRegister(
                    email = email,
                    password = password,
                    confirmPassword = authUIManager.getConfirmPassword(),
                    agreeTerms = authUIManager.isAgreeTermsChecked(),
                    onLoadingChange = { isLoading -> setLoading(isLoading) },
                    onSuccess = { LoginHandler.navigateToMain(this) }
                )
            }
        }
    }
}