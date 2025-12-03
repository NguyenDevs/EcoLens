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

    private lateinit var languageManager: LanguageManager
    private lateinit var rvLanguages: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_language_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        languageManager = LanguageManager(requireContext())

        btnBack = view.findViewById(R.id.btnBack)
        rvLanguages = view.findViewById(R.id.rvLanguages)

        setupLanguageList()

        btnBack.setOnClickListener {
            closeFragment()
        }
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
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    private fun onLanguageSelected(language: Language) {
        if (language.code != languageManager.getLanguage()) {
            languageManager.setLanguage(language.code)

            // Restart app để apply ngôn ngữ
            val intent: Intent? = requireActivity().packageManager
                .getLaunchIntentForPackage(requireActivity().packageName)

            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
                requireActivity().finish()
            }
        }
    }

    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }
}