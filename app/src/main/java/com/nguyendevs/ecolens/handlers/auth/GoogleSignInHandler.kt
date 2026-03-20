package com.nguyendevs.ecolens.handlers.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import kotlinx.coroutines.launch

/** Handler quản lý luồng đăng nhập bằng Google Sign-In. */
class GoogleSignInHandler(
        private val fragment: Fragment,
        private val userRepository: UserRepository,
        private val lifecycleScope: LifecycleCoroutineScope,
        private val languageManager: LanguageManager
) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleSignInLauncher: ActivityResultLauncher<Intent>

    private var onLoadingChange: ((Boolean) -> Unit)? = null
    private var onSuccess: (() -> Unit)? = null
    private var rememberMe: Boolean = false

    init {
        googleSignInLauncher =
                fragment.registerForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            account.idToken?.let {
                                firebaseAuthWithGoogle(
                                        it,
                                        rememberMe,
                                        onLoadingChange!!,
                                        onSuccess!!
                                )
                            }
                        } catch (e: ApiException) {
                            Toast.makeText(
                                            fragment.requireContext(),
                                            "Google sign in failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
    }

    /** Khởi tạo GoogleSignInClient với các tùy chọn đăng nhập. */
    fun setup(rememberMe: Boolean, onLoadingChange: (Boolean) -> Unit, onSuccess: () -> Unit) {
        this.rememberMe = rememberMe
        this.onLoadingChange = onLoadingChange
        this.onSuccess = onSuccess

        val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(fragment.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()

        googleSignInClient = GoogleSignIn.getClient(fragment.requireActivity(), gso)
    }

    /** Khởi chạy màn hình đăng nhập Google. */
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /** Xác thực với Firebase bằng Google ID token. */
    private fun firebaseAuthWithGoogle(
            idToken: String,
            rememberMe: Boolean,
            onLoadingChange: (Boolean) -> Unit,
            onSuccess: () -> Unit
    ) {
        onLoadingChange(true)

        lifecycleScope.launch {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = userRepository.signInWithCredential(credential)

            if (user != null) {
                saveRememberMe(true)
                userRepository.updateLanguage(languageManager.getLanguage())

                val userDetails = userRepository.getCurrentUserDetails()
                if (userDetails != null) {
                    applyUserTheme(userDetails.darkMode)
                }

                Toast.makeText(
                                fragment.requireContext(),
                                fragment.getString(R.string.login_success),
                                Toast.LENGTH_SHORT
                        )
                        .show()

                onSuccess()
            } else {
                Toast.makeText(
                                fragment.requireContext(),
                                fragment.getString(R.string.login_failed),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }

            onLoadingChange(false)
        }
    }

    /** Lưu trạng thái nhớ đăng nhập vào SharedPreferences. */
    private fun saveRememberMe(isRemember: Boolean) {
        val sharedPref =
                fragment.requireActivity()
                        .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    /** Lưu chế độ giao diện vào SharedPreferences. */
    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref =
                fragment.requireActivity()
                        .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
}
