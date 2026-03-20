package com.nguyendevs.ecolens.fragments.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.LanguageAdapter
import com.nguyendevs.ecolens.databinding.FragmentLanguageSelectionBinding
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import com.nguyendevs.ecolens.models.Language

/** Fragment chọn ngôn ngữ ứng dụng, restart app khi thay đổi ngôn ngữ. */
class LanguageSelectionFragment : Fragment() {

    private var _binding: FragmentLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageManager: LanguageManager

    /** Inflate layout của fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Khởi tạo language manager và danh sách ngôn ngữ. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        languageManager = LanguageManager(requireContext())
        setupLanguageList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Thiết lập RecyclerView hiển thị danh sách ngôn ngữ khả dụng. */
    private fun setupLanguageList() {
        val currentLang = languageManager.getLanguage()

        val languages = listOf(
            Language(
                code = LanguageManager.LANG_VI,
                name = getString(R.string.lang_vietnamese),
                flagDrawable = R.drawable.flag_vietnam,
                isSelected = currentLang == LanguageManager.LANG_VI
            ),
            Language(
                code = LanguageManager.LANG_EN,
                name = getString(R.string.lang_english),
                flagDrawable = R.drawable.flag_england,
                isSelected = currentLang == LanguageManager.LANG_EN
            ),
            Language(
                code = LanguageManager.LANG_CN,
                name = getString(R.string.lang_chinese),
                flagDrawable = R.drawable.flag_chinese,
                isSelected = currentLang == LanguageManager.LANG_CN
            ),
            Language(
                code = LanguageManager.LANG_JP,
                name = getString(R.string.lang_japanese),
                flagDrawable = R.drawable.flag_japan,
                isSelected = currentLang == LanguageManager.LANG_JP
            )
        )

        languageAdapter = LanguageAdapter(languages) { selectedLanguage ->
            binding.rvLanguages.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onLanguageSelected(selectedLanguage)
        }

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    /** Lưu ngôn ngữ mới và restart app để áp dụng thay đổi. */
    private fun onLanguageSelected(language: Language) {
        if (language.code != languageManager.getLanguage()) {
            languageManager.setLanguage(language.code)

            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("navigate_to_settings", true)
            startActivity(intent)
            requireActivity().finish()
            requireActivity().overridePendingTransition(R.anim.fade_in_2, R.anim.fade_out_2)
        }
    }

    /** Quay lại fragment trước. */
    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }
}