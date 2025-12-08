package com.nguyendevs.ecolens.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryFragment : Fragment(R.layout.screen_history) {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var adapter: HistoryAdapter
    private lateinit var btnClearFilter: ImageView
    private lateinit var btnFilterByDate: MaterialCardView
    private lateinit var btnSort: MaterialCardView
    private lateinit var emptyStateContainer: View
    private lateinit var ivExpandIcon: ImageView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var optionsHeader: FrameLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvCurrentSort: TextView
    private lateinit var tvFilterSubtitle: TextView

    private var currentSortOption = HistorySortOption.NEWEST_FIRST
    private var filterEndDate: Long? = null
    private var filterStartDate: Long? = null
    private var isOptionsExpanded = false

    // Thiết lập các thành phần sau khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapter()
        setupClickListeners()
        observeHistory()
        updateSortUI()
    }

    // Khởi tạo các view component
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

    // Thiết lập adapter cho RecyclerView
    private fun setupAdapter() {
        adapter = HistoryAdapter(
            historyList = emptyList(),
            clickListener = { entry -> navigateToDetail(entry) },
            favoriteClickListener = { entry -> viewModel.toggleFavorite(entry) }
        )
        rvHistory.adapter = adapter
    }

    // Thiết lập các listener cho button
    private fun setupClickListeners() {
        optionsHeader.setOnClickListener { toggleOptionsExpansion() }
        btnSort.setOnClickListener { toggleSortOption() }
        btnFilterByDate.setOnClickListener { showDateRangePickerDialog() }
        btnClearFilter.setOnClickListener { clearDateFilter() }
    }

    // Quan sát dữ liệu lịch sử từ ViewModel
    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate)
                .collectLatest { list ->
                    if (list.isEmpty()) {
                        rvHistory.visibility = View.GONE
                        emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        rvHistory.visibility = View.VISIBLE
                        emptyStateContainer.visibility = View.GONE
                        adapter.updateList(list)
                    }
                }
        }
    }

    // Điều hướng đến màn hình chi tiết
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

    // Chuyển đổi tùy chọn sắp xếp
    private fun toggleSortOption() {
        currentSortOption = if (currentSortOption == HistorySortOption.NEWEST_FIRST) {
            HistorySortOption.OLDEST_FIRST
        } else {
            HistorySortOption.NEWEST_FIRST
        }
        updateSortUI()
        observeHistory()
    }

    // Cập nhật giao diện sắp xếp
    private fun updateSortUI() {
        tvCurrentSort.text = if (currentSortOption == HistorySortOption.NEWEST_FIRST)
            getString(R.string.sort_newest_first)
        else
            getString(R.string.sort_oldest_first)
    }

    // Chuyển đổi trạng thái mở rộng/thu gọn của options
    private fun toggleOptionsExpansion() {
        if (isOptionsExpanded) collapseOptions() else expandOptions()
    }

    // Mở rộng phần options
    private fun expandOptions() {
        isOptionsExpanded = true
        ivExpandIcon.animate().rotation(180f).setDuration(300).start()
        optionsContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val targetHeight = optionsContainer.measuredHeight
        optionsContainer.layoutParams.height = 0
        optionsContainer.visibility = View.VISIBLE
        animateHeight(0, targetHeight)
    }

    // Thu gọn phần options
    private fun collapseOptions() {
        isOptionsExpanded = false
        ivExpandIcon.animate().rotation(0f).setDuration(300).start()
        animateHeight(optionsContainer.height, 0) { optionsContainer.visibility = View.GONE }
    }

    // Tạo animation cho chiều cao
    private fun animateHeight(from: Int, to: Int, onEnd: (() -> Unit)? = null) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.addUpdateListener { animation ->
            optionsContainer.layoutParams.height = animation.animatedValue as Int
            optionsContainer.requestLayout()
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

    // Hiển thị dialog chọn khoảng thời gian
    private fun showDateRangePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date)
            .setTheme(R.style.CustomMaterialDatePickerTheme)
            .setSelection(androidx.core.util.Pair(filterStartDate ?: MaterialDatePicker.todayInUtcMilliseconds(), filterEndDate ?: MaterialDatePicker.todayInUtcMilliseconds()))
        val picker = builder.build()
        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        picker.addOnPositiveButtonClickListener { selection ->
            filterStartDate = selection.first
            filterEndDate = selection.second + 86400000L - 1L
            tvFilterSubtitle.text = "${dateFormatter.format(selection.first)} - ${dateFormatter.format(selection.second)}"
            tvFilterSubtitle.setTextColor(resources.getColor(R.color.green_primary, null))
            btnClearFilter.visibility = View.VISIBLE
            observeHistory()
        }
    }

    // Xóa bộ lọc theo ngày
    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        tvFilterSubtitle.text = getString(R.string.select_date)
        tvFilterSubtitle.setTextColor(resources.getColor(R.color.text_secondary, null))
        btnClearFilter.visibility = View.GONE
        observeHistory()
    }
}