package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
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
import com.nguyendevs.ecolens.adapters.HistoryUiModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    
    private var currentLimit = 10
    private val pageSize = 10
    private var isLoadingMore = false
    private var hasMoreData = true
    private var observeJob: Job? = null

    enum class CategoryFilter {
        ALL,
        ANIMALS,
        PLANTS
    }

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
        
        currentLimit = pageSize
        observeHistory()
        
        updateSortUI()
        updateCategoryChipsUI(currentCategory)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapter() {
        val markwon = Markwon.builder(requireContext()).usePlugin(HtmlPlugin.create()).build()

        adapter = HistoryAdapter(
            markwon = markwon,
            clickListener = { entry ->
                animationHandler.performConfirmFeedback(binding.rvHistory)
                navigateToDetail(entry)
            }
        )
        binding.rvHistory.adapter = adapter
        binding.rvHistory.itemAnimator = null

        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMore && hasMoreData && totalItemCount <= (lastVisibleItem + 3)) {
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

        animationHandler.setupSortButtonRipple(binding.btnSort)

        binding.btnFilterByDate.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showDateRangePickerDialog()
        }

        binding.btnFilterByDate.setOnCloseIconClickListener {
            clearDateFilter()
        }
    }

    private fun updateCategoryFilter(category: CategoryFilter) {
        currentCategory = category
        updateCategoryChipsUI(category)
        currentLimit = pageSize
        observeHistory()
    }

    private fun updateCategoryChipsUI(selectedCategory: CategoryFilter) {
        animationHandler.updateMultipleChips(
            binding.chipAll to (selectedCategory == CategoryFilter.ALL),
            binding.chipAnimals to (selectedCategory == CategoryFilter.ANIMALS),
            binding.chipPlants to (selectedCategory == CategoryFilter.PLANTS)
        )
    }

    private fun observeHistory() {
        observeJob?.cancel()
        
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate, currentLimit)
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
                        if (currentLimit == pageSize) {
                            animationHandler.fadeOut(binding.rvHistory)
                            animationHandler.fadeIn(binding.emptyStateContainer)
                        }
                        hasMoreData = false
                    } else {
                        animationHandler.fadeIn(binding.rvHistory)
                        animationHandler.fadeOut(binding.emptyStateContainer)
                        
                        if (filteredList.size < currentLimit) {
                            hasMoreData = false
                        } else {
                            hasMoreData = true
                        }
                        
                        val uiModels = withContext(Dispatchers.Default) {
                            filteredList.mapIndexed { index, entry ->
                                val isFirstOfDay = index == 0 || !isSameDay(entry.timestamp, filteredList[index - 1].timestamp)
                                val isLastOfDay = index == filteredList.size - 1 || !isSameDay(entry.timestamp, filteredList[index + 1].timestamp)
                                HistoryUiModel(entry, isFirstOfDay, isLastOfDay)
                            }
                        }
                        adapter.submitList(uiModels)
                    }
                    
                    isLoadingMore = false
                    adapter.setLoading(false)
                }
        }
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault()).toLocalDate()
        return date1 == date2
    }

    private fun loadNextPage() {
        isLoadingMore = true
        adapter.setLoading(true)
        
        currentLimit += pageSize
        observeHistory()
    }

    private fun toggleSortOption() {
        currentSortOption = if (currentSortOption == HistorySortOption.NEWEST_FIRST) {
            HistorySortOption.OLDEST_FIRST
        } else {
            HistorySortOption.NEWEST_FIRST
        }
        updateSortUI()
        currentLimit = pageSize
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
        currentLimit = pageSize
        observeHistory()
    }

    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        binding.btnFilterByDate.text = getString(R.string.select_date)
        binding.btnFilterByDate.isCloseIconVisible = false

        animationHandler.updateChipStyle(binding.btnFilterByDate, false)
        currentLimit = pageSize
        observeHistory()
    }

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
