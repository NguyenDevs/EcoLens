package com.nguyendevs.ecolens.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Đăng nhập thất bại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}