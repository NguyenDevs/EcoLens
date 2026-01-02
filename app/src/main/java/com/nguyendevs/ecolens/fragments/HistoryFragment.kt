package com.nguyendevs.ecolens.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.HapticFeedbackConstants
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.databinding.ScreenHistoryModernBinding
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.view.EcoLensViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class HistoryFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: ScreenHistoryModernBinding? = null
    private val binding get() = _binding!!

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

    private lateinit var adapter: HistoryAdapter

    private var currentSortOption = HistorySortOption.NEWEST_FIRST
    private var filterEndDate: Long? = null
    private var filterStartDate: Long? = null
    private var isOptionsExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenHistoryModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupClickListeners()
        observeHistory()
        updateSortUI()
    }

    private fun setupAdapter() {
        val markwon = Markwon.builder(requireContext())
            .usePlugin(HtmlPlugin.create())
            .build()

        adapter = HistoryAdapter(
            historyList = emptyList(),
            markwon = markwon,
            clickListener = { entry ->
                binding.rvHistory.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                navigateToDetail(entry)
            },
            favoriteClickListener = { entry -> viewModel.toggleFavorite(entry) }
        )
        binding.rvHistory.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.optionsHeader.setOnClickListener {
            binding.optionsHeader.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            toggleOptionsExpansion()
        }
        binding.btnSort.setOnClickListener { toggleSortOption() }
        binding.btnFilterByDate.setOnClickListener { showDateRangePickerDialog() }
        binding.btnClearFilter.setOnClickListener { clearDateFilter() }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate)
                .collectLatest { list ->
                    if (list.isEmpty()) {
                        binding.rvHistory.visibility = View.GONE
                        binding.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.emptyStateContainer.visibility = View.GONE
                        adapter.updateList(list)
                    }
                }
        }
    }

    private fun navigateToDetail(entry: HistoryEntry) {
        val jsonEntry = Gson().toJson(entry)
        val fragment = HistoryDetailFragment().apply {
            arguments = Bundle().apply {
                putString("HISTORY_ENTRY_JSON", jsonEntry)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.hold, R.anim.hold, R.anim.slide_out_bottom)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack("Detail")
            .commit()
    }

    private fun toggleSortOption() {
        currentSortOption = if (currentSortOption == HistorySortOption.NEWEST_FIRST) {
            HistorySortOption.OLDEST_FIRST
        } else {
            HistorySortOption.NEWEST_FIRST
        }
        updateSortUI()
        observeHistory()
    }

    private fun updateSortUI() {
        binding.tvCurrentSort.text = if (currentSortOption == HistorySortOption.NEWEST_FIRST)
            getString(R.string.sort_newest_first)
        else
            getString(R.string.sort_oldest_first)
    }

    private fun toggleOptionsExpansion() {
        if (isOptionsExpanded) collapseOptions() else expandOptions()
    }

    private fun expandOptions() {
        isOptionsExpanded = true
        binding.ivExpandIcon.animate().rotation(180f).setDuration(300).start()
        binding.optionsContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val targetHeight = binding.optionsContainer.measuredHeight
        binding.optionsContainer.layoutParams.height = 0
        binding.optionsContainer.visibility = View.VISIBLE
        animateHeight(0, targetHeight)
    }

    private fun collapseOptions() {
        isOptionsExpanded = false
        binding.ivExpandIcon.animate().rotation(0f).setDuration(300).start()
        animateHeight(binding.optionsContainer.height, 0) { binding.optionsContainer.visibility = View.GONE }
    }

    private fun animateHeight(from: Int, to: Int, onEnd: (() -> Unit)? = null) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.addUpdateListener { animation ->
            binding.optionsContainer.layoutParams.height = animation.animatedValue as Int
            binding.optionsContainer.requestLayout()
        }
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 300
        onEnd?.let {
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { it() }
            })
        }
        animator.start()
    }

    private fun showDateRangePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date)
            .setTheme(R.style.CustomMaterialDatePickerTheme)
            .setSelection(androidx.core.util.Pair(
                filterStartDate ?: MaterialDatePicker.todayInUtcMilliseconds(),
                filterEndDate ?: MaterialDatePicker.todayInUtcMilliseconds()
            ))
        val picker = builder.build()

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")

        picker.addOnPositiveButtonClickListener { selection ->
            val timeZone = TimeZone.getDefault()
            val offset = timeZone.getOffset(selection.first)

            filterStartDate = selection.first - offset
            filterEndDate = (selection.second - offset) + 86400000L - 1L

            val startDate = Instant.ofEpochMilli(filterStartDate!!).atZone(ZoneId.systemDefault())
            val endDate = Instant.ofEpochMilli(filterEndDate!!).atZone(ZoneId.systemDefault())

            binding.tvFilterSubtitle.text = "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}"
            binding.tvFilterSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
            binding.btnClearFilter.visibility = View.VISIBLE
            observeHistory()
        }
    }

    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        binding.tvFilterSubtitle.text = getString(R.string.select_date)
        binding.tvFilterSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        binding.btnClearFilter.visibility = View.GONE
        observeHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}