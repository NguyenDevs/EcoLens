package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.databinding.ScreenSpeciesHistoryBinding
import com.nguyendevs.ecolens.handlers.animations.HistoryAnimationHandler
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.models.history.HistorySortOption
import com.nguyendevs.ecolens.view.EcoLensViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị lịch sử nhận diện loài
 * - Filter theo category (All/Animals/Plants)
 * - Sort (mới nhất/cũ nhất)
 * - Filter theo khoảng thời gian
 *
 * Sử dụng animation helpers:
 * - ChipAnimationHelper: Xử lý chip animations
 * - FragmentTransitionHelper: Xử lý fragment transitions
 * - ViewAnimationHelper: Xử lý haptic feedback
 */
class HistoryFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: ScreenSpeciesHistoryBinding? = null
    private val binding get() = _binding!!

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private lateinit var adapter: HistoryAdapter

    private lateinit var animationHandler: HistoryAnimationHandler

    private var currentCategory: CategoryFilter = CategoryFilter.ALL
    private var currentSortOption = HistorySortOption.NEWEST_FIRST
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null
    private var fullHistoryList: List<HistoryEntry> = emptyList()
    private var currentPage = 0
    private var isLoadingMore = false
    private val pageSize = 20

    enum class CategoryFilter {
        ALL,
        ANIMALS,
        PLANTS
    }

    // ==================== LIFECYCLE ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animationHandler = HistoryAnimationHandler(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenSpeciesHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupClickListeners()
        observeHistory()
        updateSortUI()
        updateCategoryChipsUI(currentCategory)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== UI SETUP ====================

    private fun setupAdapter() {
        val markwon = Markwon.builder(requireContext()).usePlugin(HtmlPlugin.create()).build()

        adapter = HistoryAdapter(
            historyList = mutableListOf(),
            markwon = markwon,
            clickListener = { entry ->
                animationHandler.performConfirmFeedback(binding.rvHistory)
                navigateToDetail(entry)
            }
        )
        binding.rvHistory.adapter = adapter

        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMore && totalItemCount <= (lastVisibleItem + 5)) {
                    loadNextPage()
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.chipAll.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            updateCategoryFilter(CategoryFilter.ALL)
        }

        binding.chipAnimals.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            updateCategoryFilter(CategoryFilter.ANIMALS)
        }

        binding.chipPlants.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            updateCategoryFilter(CategoryFilter.PLANTS)
        }

        binding.btnSort.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            toggleSortOption()
        }

        // Setup sort button ripple effect
        animationHandler.setupSortButtonRipple(binding.btnSort)

        binding.btnFilterByDate.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showDateRangePickerDialog()
        }

        binding.btnFilterByDate.setOnCloseIconClickListener {
            clearDateFilter()
        }
    }

    // ==================== CATEGORY FILTER ====================

    private fun updateCategoryFilter(category: CategoryFilter) {
        currentCategory = category
        updateCategoryChipsUI(category)
        observeHistory()
    }

    private fun updateCategoryChipsUI(selectedCategory: CategoryFilter) {
        animationHandler.updateMultipleChips(
            binding.chipAll to (selectedCategory == CategoryFilter.ALL),
            binding.chipAnimals to (selectedCategory == CategoryFilter.ANIMALS),
            binding.chipPlants to (selectedCategory == CategoryFilter.PLANTS)
        )
    }

    // ==================== VIEWMODEL OBSERVERS ====================

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate)
                .collectLatest { allList ->
                    val filteredList = when (currentCategory) {
                        CategoryFilter.ALL -> allList
                        CategoryFilter.ANIMALS -> allList.filter {
                            val kingdom = it.speciesInfo.kingdom.lowercase()
                            kingdom.contains("animal") || kingdom.contains("động vật")
                        }
                        CategoryFilter.PLANTS -> allList.filter {
                            val kingdom = it.speciesInfo.kingdom.lowercase()
                            kingdom.contains("plant") || kingdom.contains("thực vật")
                        }
                    }

                    if (filteredList.isEmpty()) {
                        animationHandler.fadeOut(binding.rvHistory)
                        animationHandler.fadeIn(binding.emptyStateContainer)
                    } else {
                        animationHandler.fadeIn(binding.rvHistory)
                        animationHandler.fadeOut(binding.emptyStateContainer)

                        fullHistoryList = filteredList
                        currentPage = 0
                        val firstPage = fullHistoryList.take(pageSize)
                        adapter.updateList(firstPage)
                    }
                }
        }
    }

    private fun loadNextPage() {
        val start = (currentPage + 1) * pageSize
        if (start >= fullHistoryList.size) return

        isLoadingMore = true
        adapter.setLoading(true)
        Handler(Looper.getMainLooper()).postDelayed({
            val end = (start + pageSize).coerceAtMost(fullHistoryList.size)
            val newItems = fullHistoryList.subList(start, end)

            adapter.setLoading(false)
            adapter.addItems(newItems)

            currentPage++
            isLoadingMore = false
        }, 500)
    }

    // ==================== SORT OPERATIONS ====================

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
        val sortText = if (currentSortOption == HistorySortOption.NEWEST_FIRST) {
            getString(R.string.sort_newest_first)
        } else {
            getString(R.string.sort_oldest_first)
        }
        binding.btnSort.text = sortText
    }

    // ==================== DATE FILTER OPERATIONS ====================

    private fun showDateRangePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date)
            .setTheme(R.style.CustomMaterialDatePickerTheme)
            .setSelection(
                Pair(
                    filterStartDate ?: MaterialDatePicker.todayInUtcMilliseconds(),
                    filterEndDate ?: MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
        val picker = builder.build()

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        picker.addOnPositiveButtonClickListener { selection -> applyDateFilter(selection) }
    }

    private fun applyDateFilter(selection: Pair<Long, Long>) {
        val timeZone = TimeZone.getDefault()
        val offset = timeZone.getOffset(selection.first)

        filterStartDate = selection.first - offset
        filterEndDate = (selection.second - offset) + 86400000L - 1L

        val startDate = Instant.ofEpochMilli(filterStartDate!!).atZone(ZoneId.systemDefault())
        val endDate = Instant.ofEpochMilli(filterEndDate!!).atZone(ZoneId.systemDefault())

        val dateRange = "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}"
        binding.btnFilterByDate.text = dateRange
        binding.btnFilterByDate.isCloseIconVisible = true

        animationHandler.updateChipStyle(binding.btnFilterByDate, true)
        observeHistory()
    }

    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        binding.btnFilterByDate.text = getString(R.string.select_date)
        binding.btnFilterByDate.isCloseIconVisible = false

        animationHandler.updateChipStyle(binding.btnFilterByDate, false)
        observeHistory()
    }

    // ==================== NAVIGATION ====================

    private fun navigateToDetail(entry: HistoryEntry) {
        val jsonEntry = Gson().toJson(entry)
        val fragment = HistoryDetailFragment().apply {
            arguments = Bundle().apply {
                putString("HISTORY_ENTRY_JSON", jsonEntry)
            }
        }

        parentFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.hold,
                R.anim.hold,
                R.anim.slide_out_bottom
            )
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack("Detail")
            .commit()
    }
}