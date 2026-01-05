package com.nguyendevs.ecolens.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding
import com.nguyendevs.ecolens.model.User
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLoginStatus()

        setupTabs()
        setupButtons()
    }

    private fun checkLoginStatus() {
        val sharedPreferences = getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        if (username != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupTabs() {
        binding.tabLayoutAuth.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Login
                        binding.tilConfirmPassword.visibility = View.GONE
                        binding.cbAgreeTerms.visibility = View.GONE
                        binding.btnAuthAction.text = getString(R.string.login)
                    }
                    1 -> { // Register
                        binding.tilConfirmPassword.visibility = View.VISIBLE
                        binding.cbAgreeTerms.visibility = View.VISIBLE
                        binding.btnAuthAction.text = getString(R.string.register)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        binding.btnAuthAction.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val isLogin = binding.tabLayoutAuth.selectedTabPosition == 0

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressIndicator.visibility = View.VISIBLE
            binding.btnAuthAction.isEnabled = false

            lifecycleScope.launch {
                if (isLogin) {
                    val passwordHash = userRepository.hashPassword(password)
                    // Assuming email is used as username for simplicity, or extract username from email
                    val username = email.substringBefore("@") 
                    val user = userRepository.loginUser(username, passwordHash)
                    if (user != null) {
                        saveLoginState(user.username)
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@AuthActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val confirmPassword = binding.etConfirmPassword.text.toString()
                    if (password != confirmPassword) {
                        Toast.makeText(this@AuthActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnAuthAction.isEnabled = true
                        return@launch
                    }
                    
                    if (!binding.cbAgreeTerms.isChecked) {
                         Toast.makeText(this@AuthActivity, "Please agree to terms", Toast.LENGTH_SHORT).show()
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnAuthAction.isEnabled = true
                        return@launch
                    }

                    val username = email.substringBefore("@")
                    val passwordHash = userRepository.hashPassword(password)
                    val newUser = User(username, email, passwordHash)
                    
                    if (userRepository.registerUser(newUser)) {
                        saveLoginState(username)
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@AuthActivity, "Registration failed or user exists", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.progressIndicator.visibility = View.GONE
                binding.btnAuthAction.isEnabled = true
            }
        }
    }

    private fun saveLoginState(username: String) {
        val sharedPreferences = getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("username", username).apply()
    }
}