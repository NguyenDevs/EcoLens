package com.nguyendevs.ecolens.fragments

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
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.model.Language

/**
 * Fragment cho phép người dùng chọn ngôn ngữ ứng dụng
 * Khi chọn ngôn ngữ mới, app sẽ restart để áp dụng thay đổi
 */
class LanguageSelectionFragment : Fragment() {

    private var _binding: FragmentLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageManager: LanguageManager

    // ==================== LIFECYCLE ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        languageManager = LanguageManager(requireContext())
        setupLanguageList()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== UI SETUP ====================

    /**
     * Cấu hình danh sách ngôn ngữ với adapter
     * Highlight ngôn ngữ hiện tại được chọn
     */
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

    /**
     * Cấu hình click listeners
     */
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            closeFragment()
        }
    }

    // ==================== LANGUAGE SELECTION ====================

    /**
     * Xử lý khi người dùng chọn ngôn ngữ mới
     * Restart app để áp dụng thay đổi và quay về Settings
     */
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

    // ==================== NAVIGATION ====================

    /**
     * Đóng fragment và quay lại màn hình trước
     */
    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }
}