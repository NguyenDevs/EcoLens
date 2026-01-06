package com.nguyendevs.ecolens.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nguyendevs.ecolens.databinding.FragmentAboutModernBinding

/**
 * Fragment hiển thị thông tin về ứng dụng
 * Bao gồm version, mô tả và credits
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutModernBinding? = null
    private val binding get() = _binding!!

    // ==================== LIFECYCLE ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== UI SETUP ====================

    /**
     * Thiết lập click listeners cho UI elements
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}