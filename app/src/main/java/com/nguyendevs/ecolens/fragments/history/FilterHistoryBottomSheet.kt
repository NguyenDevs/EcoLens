package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetFilterHistoryBinding
import com.nguyendevs.ecolens.models.history.HistorySortOption

class FilterHistoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetFilterHistoryBinding? = null
    private val binding get() = _binding!!

    private var selectedSortOption: HistorySortOption = HistorySortOption.NEWEST_FIRST

    var onApplyListener: ((HistorySortOption) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
        
        arguments?.let {
            selectedSortOption = HistorySortOption.valueOf(it.getString(ARG_SORT, "NEWEST_FIRST"))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetFilterHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateSortUI()
        setupListeners()
    }

    private fun setupListeners() {
        binding.rowNewest.setOnClickListener { selectOption(HistorySortOption.NEWEST_FIRST) }
        binding.rowOldest.setOnClickListener { selectOption(HistorySortOption.OLDEST_FIRST) }
        binding.rowAlphabet.setOnClickListener { selectOption(HistorySortOption.ALPHABETICAL) }
        binding.rowConfidence.setOnClickListener { selectOption(HistorySortOption.CONFIDENCE_HIGH) }
        binding.rowFavorite.setOnClickListener { selectOption(HistorySortOption.FAVORITE) }
    }

    private fun selectOption(option: HistorySortOption) {
        selectedSortOption = option
        updateSortUI()
        onApplyListener?.invoke(selectedSortOption)
        dismiss()
    }

    private fun updateSortUI() {
        resetSortRow(binding.rowNewest, binding.indicatorNewest)
        resetSortRow(binding.rowOldest, binding.indicatorOldest)
        resetSortRow(binding.rowAlphabet, binding.indicatorAlphabet)
        resetSortRow(binding.rowConfidence, binding.indicatorConfidence)
        resetSortRow(binding.rowFavorite, binding.indicatorFavorite)

        when (selectedSortOption) {
            HistorySortOption.NEWEST_FIRST -> setActiveSortRow(binding.rowNewest, binding.indicatorNewest)
            HistorySortOption.OLDEST_FIRST -> setActiveSortRow(binding.rowOldest, binding.indicatorOldest)
            HistorySortOption.ALPHABETICAL -> setActiveSortRow(binding.rowAlphabet, binding.indicatorAlphabet)
            HistorySortOption.CONFIDENCE_HIGH -> setActiveSortRow(binding.rowConfidence, binding.indicatorConfidence)
            HistorySortOption.FAVORITE -> setActiveSortRow(binding.rowFavorite, binding.indicatorFavorite)
        }
    }

    private fun resetSortRow(row: View, indicator: View) {
        row.setBackgroundResource(R.drawable.bg_sort_option_normal)
        indicator.setBackgroundResource(R.drawable.ic_radio_unchecked)
        val tv = (row as ViewGroup).getChildAt(0) as? TextView
        tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        tv?.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    private fun setActiveSortRow(row: View, indicator: View) {
        row.setBackgroundResource(R.drawable.bg_sort_option_active)
        indicator.setBackgroundResource(R.drawable.ic_radio_checked)
        val tv = (row as ViewGroup).getChildAt(0) as? TextView
        tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        tv?.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterHistoryBottomSheet"
        private const val ARG_SORT = "arg_sort"

        fun newInstance(sort: HistorySortOption): FilterHistoryBottomSheet {
            return FilterHistoryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort.name)
                }
            }
        }
    }
}
