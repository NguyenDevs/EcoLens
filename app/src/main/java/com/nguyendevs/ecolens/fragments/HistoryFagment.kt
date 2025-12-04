package com.nguyendevs.ecolens.fragments

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment(R.layout.screen_history) {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private lateinit var adapter: HistoryAdapter
    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var btnSort: MaterialCardView
    private lateinit var btnFilterByDate: MaterialCardView
    private lateinit var tvCurrentSort: TextView
    private lateinit var tvFilterSubtitle: TextView
    private lateinit var btnClearFilter: ImageView
    private lateinit var optionsHeader: LinearLayout
    private lateinit var optionsContainer: LinearLayout
    private lateinit var ivExpandIcon: ImageView

    private var currentSortOption = HistorySortOption.NEWEST_FIRST
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null
    private var isOptionsExpanded = false

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAdapter()
        observeHistory()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        rvHistory = view.findViewById(R.id.rvHistory)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        btnSort = view.findViewById(R.id.btnSort)
        btnFilterByDate = view.findViewById(R.id.btnFilterByDate)
        tvCurrentSort = view.findViewById(R.id.tvCurrentSort)
        tvFilterSubtitle = view.findViewById(R.id.tvFilterSubtitle)
        btnClearFilter = view.findViewById(R.id.btnClearFilter)
        optionsHeader = view.findViewById(R.id.optionsHeader)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        ivExpandIcon = view.findViewById(R.id.ivExpandIcon)
    }

    private fun setupAdapter() {
        adapter = HistoryAdapter(
            historyList = emptyList(),
            clickListener = { entry ->
                showHistoryDetail(entry)
            },
            favoriteClickListener = { entry ->
                viewModel.toggleFavorite(entry)
            }
        )
        rvHistory.adapter = adapter
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption).collect { list ->
                val filteredList = if (filterStartDate != null && filterEndDate != null) {
                    list.filter { it.timestamp in filterStartDate!!..filterEndDate!! }
                } else {
                    list
                }

                if (filteredList.isEmpty()) {
                    rvHistory.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                } else {
                    rvHistory.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE
                    adapter.updateList(filteredList)
                }
            }
        }
    }

    private fun setupClickListeners() {
        optionsHeader.setOnClickListener {
            toggleOptionsExpansion()
        }

        btnSort.setOnClickListener {
            showSortDialog()
        }

        btnFilterByDate.setOnClickListener {
            showDateRangePickerDialog()
        }

        btnClearFilter.setOnClickListener {
            clearDateFilter()
        }
    }

    private fun toggleOptionsExpansion() {
        if (isOptionsExpanded) {
            collapseOptions()
        } else {
            expandOptions()
        }
    }

    private fun expandOptions() {
        isOptionsExpanded = true

        // Rotate icon
        ivExpandIcon.animate()
            .rotation(180f)
            .setDuration(300)
            .start()

        // Measure the target height
        optionsContainer.measure(
            View.MeasureSpec.makeMeasureSpec(optionsContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = optionsContainer.measuredHeight

        // Animate height from 0 to target
        optionsContainer.layoutParams.height = 0
        optionsContainer.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = optionsContainer.layoutParams
            layoutParams.height = value
            optionsContainer.layoutParams = layoutParams
        }
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 300
        animator.start()
    }

    private fun collapseOptions() {
        isOptionsExpanded = false

        // Rotate icon back
        ivExpandIcon.animate()
            .rotation(0f)
            .setDuration(300)
            .start()

        val initialHeight = optionsContainer.height
        val animator = ValueAnimator.ofInt(initialHeight, 0)

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = optionsContainer.layoutParams
            layoutParams.height = value
            optionsContainer.layoutParams = layoutParams
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                optionsContainer.visibility = View.GONE
                optionsContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        })

        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 300
        animator.start()
    }

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.sort_newest_first),
            getString(R.string.sort_oldest_first)
        )

        val currentSelection = when (currentSortOption) {
            HistorySortOption.NEWEST_FIRST -> 0
            HistorySortOption.OLDEST_FIRST -> 1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sort_by))
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                currentSortOption = when (which) {
                    0 -> HistorySortOption.NEWEST_FIRST
                    1 -> HistorySortOption.OLDEST_FIRST
                    else -> HistorySortOption.NEWEST_FIRST
                }

                tvCurrentSort.text = options[which]
                observeHistory()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDateRangePickerDialog() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startCalendar = Calendar.getInstance()
                startCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startDate = startCalendar.timeInMillis

                showEndDatePicker(startDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(getString(R.string.select_start_date))
            show()
        }
    }

    private fun showEndDatePicker(startDate: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val endCalendar = Calendar.getInstance()
                endCalendar.set(year, month, dayOfMonth, 23, 59, 59)
                endCalendar.set(Calendar.MILLISECOND, 999)
                val endDate = endCalendar.timeInMillis

                if (endDate < startDate) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.end_date_before_start_date_error))
                        .setPositiveButton(getString(R.string.ok), null)
                        .show()
                } else {
                    applyDateFilter(startDate, endDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(getString(R.string.select_end_date))
            datePicker.minDate = startDate
            show()
        }
    }

    private fun applyDateFilter(startDate: Long, endDate: Long) {
        filterStartDate = startDate
        filterEndDate = endDate

        val startDateStr = dateFormatter.format(startDate)
        val endDateStr = dateFormatter.format(endDate)

        // Update subtitle with filter info
        tvFilterSubtitle.text = "$startDateStr - $endDateStr"
        tvFilterSubtitle.setTextColor(resources.getColor(R.color.green_primary, null))

        // Show clear button
        btnClearFilter.visibility = View.VISIBLE

        observeHistory()
    }

    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null

        // Reset subtitle
        tvFilterSubtitle.text = "Chọn khoảng thời gian"
        tvFilterSubtitle.setTextColor(resources.getColor(R.color.text_secondary, null))

        // Hide clear button
        btnClearFilter.visibility = View.GONE

        observeHistory()
    }

    private fun showHistoryDetail(entry: HistoryEntry) {
        val detailFragment = HistoryDetailFragment()
        detailFragment.setData(entry)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, detailFragment)
            .addToBackStack(null)
            .commit()

        val fragmentContainer = activity?.findViewById<View>(R.id.fragmentContainer)
        fragmentContainer?.visibility = View.VISIBLE
    }
}