package com.nguyendevs.ecolens.handlers.setting

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.fragments.setting.AboutFragment
import com.nguyendevs.ecolens.fragments.setting.LanguageSelectionFragment
import com.nguyendevs.ecolens.managers.setting.LanguageManager

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
                openFragment(LanguageSelectionFragment(), "language_selection")
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

    /** Cập nhật hiển thị ngôn ngữ hiện tại */
    fun updateLanguageDisplay() {
        val currentLang = languageManager.getLanguage()
        binding.tvCurrentLanguage.text =
                when (currentLang) {
                    LanguageManager.LANG_EN -> activity.getString(R.string.lang_english)
                    LanguageManager.LANG_VI -> activity.getString(R.string.lang_vietnamese)
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
