package com.nguyendevs.ecolens.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nguyendevs.ecolens.ui.screens.auth.LoginScreen
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme

/** Compose-based Authentication Activity. Replaces AuthActivity for full Compose experience. */
class ComposeAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var emailError by remember { mutableStateOf<String?>(null) }
            var passwordError by remember { mutableStateOf<String?>(null) }
            var confirmPasswordError by remember { mutableStateOf<String?>(null) }

            EcoLensTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                            onLoginClick = { email, password, rememberMe ->
                                // Validate and login
                                emailError = validateEmail(email)
                                passwordError = validatePassword(password)

                                if (emailError == null && passwordError == null) {
                                    // TODO: Perform login with Firebase
                                    performLogin(email, password, rememberMe)
                                }
                            },
                            onRegisterClick = { email, password, confirmPassword, agreeTerms ->
                                // Validate and register
                                emailError = validateEmail(email)
                                passwordError = validatePassword(password)
                                confirmPasswordError =
                                        if (password != confirmPassword) {
                                            "Mật khẩu không khớp"
                                        } else null

                                if (emailError == null &&
                                                passwordError == null &&
                                                confirmPasswordError == null &&
                                                agreeTerms
                                ) {
                                    // TODO: Perform registration with Firebase
                                    performRegister(email, password)
                                }
                            },
                            onForgotPasswordClick = {
                                // TODO: Navigate to forgot password
                            },
                            onGoogleSignInClick = {
                                // TODO: Perform Google sign in
                            },
                            onBiometricClick = {
                                // TODO: Perform biometric authentication
                            },
                            showBiometric = false, // TODO: Check if biometric is available
                            emailError = emailError,
                            passwordError = passwordError,
                            confirmPasswordError = confirmPasswordError
                    )
                }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Vui lòng nhập email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không hợp lệ"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Vui lòng nhập mật khẩu"
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
            else -> null
        }
    }

    private fun performLogin(email: String, password: String, rememberMe: Boolean) {
        // TODO: Implement Firebase login
        // On success: navigate to ComposeMainActivity
        // On failure: show error
    }

    private fun performRegister(email: String, password: String) {
        // TODO: Implement Firebase registration
        // On success: navigate to ComposeMainActivity
        // On failure: show error
    }
}
