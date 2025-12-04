package com.nguyendevs.ecolens.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
    private lateinit var btnSort: ImageView
    private lateinit var btnFilterByDate: ImageView
    private lateinit var filterInfoCard: MaterialCardView
    private lateinit var tvFilterInfo: TextView
    private lateinit var btnClearFilter: ImageView

    private var currentSortOption = HistorySortOption.NEWEST_FIRST
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        btnSort = view.findViewById(R.id.btnSort)
        btnFilterByDate = view.findViewById(R.id.btnFilterByDate)
        filterInfoCard = view.findViewById(R.id.filterInfoCard)
        tvFilterInfo = view.findViewById(R.id.tvFilterInfo)
        btnClearFilter = view.findViewById(R.id.btnClearFilter)

        setupAdapter()
        observeHistory()
        setupClickListeners()
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

        // Chọn ngày bắt đầu
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startCalendar = Calendar.getInstance()
                startCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startDate = startCalendar.timeInMillis

                // Sau khi chọn ngày bắt đầu, chọn ngày kết thúc
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
                    // Hiển thị lỗi nếu ngày kết thúc < ngày bắt đầu
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
        tvFilterInfo.text = getString(R.string.filter_date_range, startDateStr, endDateStr)
        filterInfoCard.visibility = View.VISIBLE

        observeHistory()
    }

    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        filterInfoCard.visibility = View.GONE
        observeHistory()
    }

    private fun showHistoryDetail(entry: HistoryEntry) {
        val detailFragment = HistoryDetailFragment()
        detailFragment.setData(entry)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, detailFragment)
            .addToBackStack(null)
            .commit()

        // Hiển thị fragment container
        val fragmentContainer = activity?.findViewById<View>(R.id.fragmentContainer)
        fragmentContainer?.visibility = View.VISIBLE
    }
}