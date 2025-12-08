package com.nguyendevs.ecolens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.LanguageAdapter
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.model.Language

class LanguageSelectionFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageManager: LanguageManager
    private lateinit var rvLanguages: RecyclerView

    // Tạo view cho Fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_language_selection, container, false)
    }

    // Thiết lập các thành phần sau khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        languageManager = LanguageManager(requireContext())

        initViews(view)
        setupLanguageList()
        setupListeners()
    }

    // Khởi tạo các view component
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvLanguages = view.findViewById(R.id.rvLanguages)
    }

    // Thiết lập danh sách ngôn ngữ
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
            onLanguageSelected(selectedLanguage)
        }

        rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    // Thiết lập các listener cho button
    private fun setupListeners() {
        btnBack.setOnClickListener {
            closeFragment()
        }
    }

    // Xử lý khi người dùng chọn ngôn ngữ mới
    private fun onLanguageSelected(language: Language) {
        if (language.code != languageManager.getLanguage()) {
            languageManager.setLanguage(language.code)

            val intent: Intent? = requireActivity().packageManager
                .getLaunchIntentForPackage(requireActivity().packageName)

            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
                requireActivity().finish()
            }
        }
    }

    // Đóng Fragment
    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }
}