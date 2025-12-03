package com.nguyendevs.ecolens.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.LanguageAdapter
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.model.Language

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var languageManager: LanguageManager
    private lateinit var rvLanguages: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var languageAdapter: LanguageAdapter

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LanguageSelectionActivity::class.java)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)

        initViews()
        setupLanguageList()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rvLanguages = findViewById(R.id.rvLanguages)
    }

    private fun setupLanguageList() {
        val currentLang = languageManager.getLanguage()

        val languages = listOf(
            Language(
                code = LanguageManager.LANG_EN,
                name = getString(R.string.lang_english),
                flagEmoji = "🇺🇸",
                isSelected = currentLang == LanguageManager.LANG_EN
            ),
            Language(
                code = LanguageManager.LANG_VI,
                name = getString(R.string.lang_vietnamese),
                flagEmoji = "🇻🇳",
                isSelected = currentLang == LanguageManager.LANG_VI
            )
        )

        languageAdapter = LanguageAdapter(languages) { selectedLanguage ->
            onLanguageSelected(selectedLanguage)
        }

        rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageSelectionActivity)
            adapter = languageAdapter
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun onLanguageSelected(language: Language) {
        if (language.code != languageManager.getLanguage()) {
            languageManager.setLanguage(language.code)

            // Restart app to apply language
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.hold_scale, R.anim.scale_out)
    }
}