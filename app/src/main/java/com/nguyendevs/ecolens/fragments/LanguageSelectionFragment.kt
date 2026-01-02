package com.nguyendevs.ecolens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.LanguageAdapter
import com.nguyendevs.ecolens.databinding.FragmentLanguageSelectionModernBinding
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.model.Language

class LanguageSelectionFragment : Fragment() {

    private var _binding: FragmentLanguageSelectionModernBinding? = null
    private val binding get() = _binding!!

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageManager: LanguageManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSelectionModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        languageManager = LanguageManager(requireContext())
        setupLanguageList()
        setupListeners()
    }

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

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            closeFragment()
        }
    }

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

    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}