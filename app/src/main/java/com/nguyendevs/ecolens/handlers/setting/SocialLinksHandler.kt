package com.nguyendevs.ecolens.handlers.setting

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.LanguageAdapter
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.fragments.setting.AboutFragment
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import com.nguyendevs.ecolens.models.Language

/**
 * Handler quản lý social links, email feedback, navigation và language display. Xử lý: open URLs,
 * send email, open fragments, update language display.
 */
class SocialLinksHandler(
        private val activity: AppCompatActivity,
        private val binding: ScreenSettingsBinding,
        private val languageManager: LanguageManager
) {

    init {
        updateLanguageDisplay()
    }

    /** Setup click listeners cho social links và navigation options */
    fun setupClickListeners(
            onLanguageClick: () -> Unit = {
                showLanguageSelectionDialog()
            },
            onAboutClick: () -> Unit = { openFragment(AboutFragment(), "about_screen") }
    ) {
        binding.languageOption.setOnClickListener { onLanguageClick() }
        binding.aboutOption.setOnClickListener { onAboutClick() }

        binding.btnFeedback.setOnClickListener { sendEmail() }
        binding.btnFacebook.setOnClickListener { openUrl("https://www.facebook.com/NguyenDevs") }
        binding.btnInstagram.setOnClickListener { openUrl("https://www.instagram.com/nguyendevs/") }
        binding.btnTiktok.setOnClickListener { openUrl("https://www.tiktok.com/@nguyendevs/") }
    }

    private fun showLanguageSelectionDialog() {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_language_selection, null)
        dialog.setContentView(view)

        val rvLanguages = view.findViewById<RecyclerView>(R.id.rvLanguages)
        val currentLang = languageManager.getLanguage()

        val languages = listOf(
            Language(
                code = LanguageManager.LANG_VI,
                name = activity.getString(R.string.lang_vietnamese),
                flagDrawable = R.drawable.flag_vietnam,
                isSelected = currentLang == LanguageManager.LANG_VI
            ),
            Language(
                code = LanguageManager.LANG_EN,
                name = activity.getString(R.string.lang_english),
                flagDrawable = R.drawable.flag_england,
                isSelected = currentLang == LanguageManager.LANG_EN
            ),
            Language(
                code = LanguageManager.LANG_CN,
                name = activity.getString(R.string.lang_chinese),
                flagDrawable = R.drawable.flag_chinese,
                isSelected = currentLang == LanguageManager.LANG_CN
            ),
            Language(
                code = LanguageManager.LANG_JP,
                name = activity.getString(R.string.lang_japanese),
                flagDrawable = R.drawable.flag_japan,
                isSelected = currentLang == LanguageManager.LANG_JP
            )
        )

        val adapter = LanguageAdapter(languages) { selectedLanguage ->
            if (selectedLanguage.code != languageManager.getLanguage()) {
                languageManager.setLanguage(selectedLanguage.code)
                dialog.dismiss()
                restartApp()
            } else {
                dialog.dismiss()
            }
        }

        rvLanguages.layoutManager = LinearLayoutManager(activity)
        rvLanguages.adapter = adapter

        dialog.show()
    }

    private fun restartApp() {
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("navigate_to_settings", true)
        activity.startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(R.anim.fade_in_2, R.anim.fade_out_2)
    }

    /** Cập nhật hiển thị ngôn ngữ hiện tại */
    fun updateLanguageDisplay() {
        val currentLang = languageManager.getLanguage()
        binding.tvCurrentLanguage.text =
                when (currentLang) {
                    LanguageManager.LANG_EN -> activity.getString(R.string.lang_english)
                    LanguageManager.LANG_VI -> activity.getString(R.string.lang_vietnamese)
                    LanguageManager.LANG_CN -> activity.getString(R.string.lang_chinese)
                    LanguageManager.LANG_JP -> activity.getString(R.string.lang_japanese)
                    else -> activity.getString(R.string.lang_english)
                }
    }

    /** Mở fragment mới */
    fun openFragment(fragment: Fragment, tag: String) {
        val fragmentContainer = activity.findViewById<FrameLayout>(R.id.fragmentContainer)
        fragmentContainer.visibility = View.VISIBLE

        activity.supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in_2,
                        R.anim.fade_out_2,
                        R.anim.fade_in_2,
                        R.anim.fade_out_2
                )
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(tag)
                .commit()
    }

    /** Mở URL trong browser */
    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity.startActivity(intent)
        }
                .onFailure {
                    Toast.makeText(
                                    activity,
                                    activity.getString(R.string.error_open_link),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
    }

    /** Mở email app để gửi feedback */
    fun sendEmail() {
        runCatching {
            val intent =
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:tainguyen.devs@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "EcoLens Support")
                    }
            activity.startActivity(intent)
        }
                .onFailure {
                    Toast.makeText(
                                    activity,
                                    activity.getString(R.string.error_no_email_app),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
    }
}