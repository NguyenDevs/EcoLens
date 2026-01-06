package com.nguyendevs.ecolens.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.GoogleAuthProvider
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch

/**
 * Activity xử lý đăng nhập và đăng ký người dùng
 * Hỗ trợ email/password và Google Sign-In
 * Tự động chuyển đến MainActivity nếu đã đăng nhập và có "Remember Me"
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val userRepository = UserRepository()
    private lateinit var googleSignInClient: GoogleSignInClient

    /**
     * Activity result launcher cho Google Sign-In
     */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== LIFECYCLE ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkAutoLogin()) {
            return
        }

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupTabs()
        setupButtons()
    }

    // ==================== AUTO LOGIN ====================

    /**
     * Kiểm tra và tự động đăng nhập nếu có "Remember Me"
     * @return true nếu đã auto login và finish activity
     */
    private fun checkAutoLogin(): Boolean {
        if (userRepository.isUserLoggedIn()) {
            val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val rememberMe = sharedPref.getBoolean("remember_me", false)

            if (rememberMe) {
                val isDarkMode = sharedPref.getBoolean("dark_mode", false)
                val nightMode = if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)

                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.fade_in_3, R.anim.fade_out_3)
                finish()
                return true
            } else {
                userRepository.logout()
            }
        }
        return false
    }

    // ==================== GOOGLE SIGN-IN ====================

    /**
     * Cấu hình Google Sign-In client
     */
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    /**
     * Xác thực với Firebase sử dụng Google ID token
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.btnGoogle.isEnabled = false

        lifecycleScope.launch {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = userRepository.signInWithCredential(credential)

            if (user != null) {
                saveRememberMe(binding.cbRememberMe.isChecked)

                val userDetails = userRepository.getCurrentUserDetails()
                if (userDetails != null) {
                    applyUserTheme(userDetails.darkMode)
                }
                Toast.makeText(
                    this@AuthActivity,
                    getString(R.string.login_success),
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMain()
            } else {
                Toast.makeText(
                    this@AuthActivity,
                    getString(R.string.login_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.progressIndicator.visibility = View.GONE
            binding.btnGoogle.isEnabled = true
        }
    }

    // ==================== UI SETUP ====================

    /**
     * Cấu hình tabs Login/Register với UI thay đổi theo tab
     */
    private fun setupTabs() {
        binding.tabLayoutAuth.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.tilConfirmPassword.visibility = View.GONE
                        binding.cbAgreeTerms.visibility = View.GONE
                        (binding.cbRememberMe.parent as? View)?.visibility = View.VISIBLE
                        binding.btnAuthAction.text = getString(R.string.login)
                    }
                    1 -> {
                        (binding.cbRememberMe.parent as? View)?.visibility = View.GONE
                        binding.cbAgreeTerms.visibility = View.VISIBLE
                        binding.tilConfirmPassword.visibility = View.VISIBLE
                        binding.btnAuthAction.text = getString(R.string.register)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * Cấu hình button đăng nhập/đăng ký
     * Xử lý cả login và register logic dựa vào tab được chọn
     */
    private fun setupButtons() {
        binding.btnAuthAction.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val isLogin = binding.tabLayoutAuth.selectedTabPosition == 0

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(
                    this,
                    getString(R.string.error_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            setLoading(true)

            lifecycleScope.launch {
                if (isLogin) {
                    handleLogin(email, password)
                } else {
                    handleRegister(email, password)
                }
                setLoading(false)
            }
        }
    }

    // ==================== AUTHENTICATION LOGIC ====================

    /**
     * Xử lý đăng nhập với email và password
     */
    private suspend fun handleLogin(email: String, password: String) {
        val firebaseUser = userRepository.loginUser(email, password)
        if (firebaseUser != null) {
            saveRememberMe(binding.cbRememberMe.isChecked)

            val userDetails = userRepository.getCurrentUserDetails()
            if (userDetails != null) {
                applyUserTheme(userDetails.darkMode)
            }

            Toast.makeText(
                this@AuthActivity,
                getString(R.string.login_success),
                Toast.LENGTH_SHORT
            ).show()
            navigateToMain()
        } else {
            Toast.makeText(
                this@AuthActivity,
                getString(R.string.login_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Xử lý đăng ký tài khoản mới
     */
    private suspend fun handleRegister(email: String, password: String) {
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        if (password != confirmPassword) {
            Toast.makeText(
                this@AuthActivity,
                getString(R.string.error_password_mismatch),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!binding.cbAgreeTerms.isChecked) {
            Toast.makeText(
                this@AuthActivity,
                getString(R.string.error_terms_not_accepted),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val username = email.substringBefore("@")

        if (userRepository.registerUser(email, password, username)) {
            saveRememberMe(true)
            Toast.makeText(
                this@AuthActivity,
                getString(R.string.register_success),
                Toast.LENGTH_SHORT
            ).show()
            applyUserTheme(false)
            navigateToMain()
        } else {
            Toast.makeText(
                this@AuthActivity,
                getString(R.string.register_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Lưu trạng thái "Remember Me" vào SharedPreferences
     */
    private fun saveRememberMe(isRemember: Boolean) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    /**
     * Áp dụng theme (Dark/Light mode) cho app
     */
    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Bật/tắt trạng thái loading
     */
    private fun setLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAuthAction.isEnabled = !isLoading
    }

    /**
     * Chuyển đến MainActivity với fade animation
     */
    private fun navigateToMain() {
        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in_3, R.anim.fade_out_3)
        finish()
    }
}