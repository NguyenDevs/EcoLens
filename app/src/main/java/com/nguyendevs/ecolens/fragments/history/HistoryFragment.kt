package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.HistoryAdapter
import com.nguyendevs.ecolens.adapters.HistoryUiModel
import com.nguyendevs.ecolens.adapters.HistoryViewMode
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
    private var searchQuery: String = ""
    private var currentViewMode = HistoryViewMode.GRID

    private var currentLimit = 10
    private val pageSize = 10
    private var isLoadingMore = false
    private var hasMoreData = true
    private var observeJob: Job? = null

    enum class CategoryFilter { ALL, ANIMALS, PLANTS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animationHandler = HistoryAnimationHandler(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ScreenSpeciesHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupSearch()
        setupClickListeners()
        applyViewMode(currentViewMode)
        currentLimit = pageSize
        observeHistory()
        updateSortUI()
        updateCategoryChipsUI(currentCategory)
        observeLoadingState()
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isHistoryLoading.collectLatest { isLoading ->
                if (isLoading) {
                    binding.shimmerViewContainer.startShimmer()
                    binding.shimmerViewContainer.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.GONE
                } else {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapter() {
        val markwon = Markwon.builder(requireContext()).usePlugin(HtmlPlugin.create()).build()
        adapter = HistoryAdapter(markwon = markwon, clickListener = { entry ->
            animationHandler.performConfirmFeedback(binding.rvHistory)
            navigateToDetail(entry)
        })
        binding.rvHistory.adapter = adapter
        binding.rvHistory.itemAnimator = null

        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val scrollY = recyclerView.computeVerticalScrollOffset()
                binding.stickyHeader.elevation = if (scrollY > 10) 8f else 0f

                if (dy <= 0) return
                val lm = recyclerView.layoutManager
                val total = lm?.itemCount ?: return
                val lastVisible = when (lm) {
                    is LinearLayoutManager -> lm.findLastVisibleItemPosition()
                    is GridLayoutManager -> lm.findLastVisibleItemPosition()
                    else -> return
                }
                if (!isLoadingMore && hasMoreData && total <= lastVisible + 3) loadNextPage()
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                binding.ivSearchClear.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                currentLimit = pageSize
                observeHistory()
            }
        })
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }
        binding.ivSearchClear.setOnClickListener {
            binding.etSearch.text?.clear()
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
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
        binding.btnFilterByDate.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showDateRangePickerDialog()
        }
        binding.btnFilterByDate.setOnCloseIconClickListener { clearDateFilter() }

        binding.btnViewList.setOnClickListener {
            if (currentViewMode != HistoryViewMode.LIST) {
                currentViewMode = HistoryViewMode.LIST
                applyViewMode(currentViewMode)
            }
        }
        binding.btnViewGrid.setOnClickListener {
            if (currentViewMode != HistoryViewMode.GRID) {
                currentViewMode = HistoryViewMode.GRID
                applyViewMode(currentViewMode)
            }
        }
    }

    private fun applyViewMode(mode: HistoryViewMode) {
        adapter.viewMode = mode
        if (mode == HistoryViewMode.GRID) {
            binding.rvHistory.layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (adapter.getItemViewType(position) == 2) 2
                        else if (position < adapter.itemCount && adapter.currentList.getOrNull(position)?.isFirstOfDay == true) 2
                        else 1
                    }
                }
            }
            binding.btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_active)
            binding.btnViewGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnViewList.setBackgroundResource(R.drawable.bg_view_toggle)
            binding.btnViewList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
        } else {
            binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
            binding.btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_active)
            binding.btnViewList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle)
            binding.btnViewGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
        }
    }

    private fun updateCategoryFilter(category: CategoryFilter) {
        currentCategory = category
        updateCategoryChipsUI(category)
        currentLimit = pageSize
        observeHistory()
    }

    private fun updateCategoryChipsUI(selected: CategoryFilter) {
        animationHandler.updateMultipleChips(
            binding.chipAll to (selected == CategoryFilter.ALL),
            binding.chipAnimals to (selected == CategoryFilter.ANIMALS),
            binding.chipPlants to (selected == CategoryFilter.PLANTS)
        )
    }

    private fun observeHistory() {
        observeJob?.cancel()
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate, currentLimit)
                .collectLatest { allList ->
                    var filtered = when (currentCategory) {
                        CategoryFilter.ALL -> allList
                        CategoryFilter.ANIMALS -> allList.filter {
                            val k = it.speciesInfo.kingdom.lowercase()
                            k.contains("animal") || k.contains("động vật")
                        }
                        CategoryFilter.PLANTS -> allList.filter {
                            val k = it.speciesInfo.kingdom.lowercase()
                            k.contains("plant") || k.contains("thực vật")
                        }
                    }

                    if (searchQuery.isNotEmpty()) {
                        filtered = filtered.filter {
                            it.speciesInfo.commonName.contains(searchQuery, ignoreCase = true) ||
                                    it.speciesInfo.scientificName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    binding.tvResultCount.text = "${filtered.size} kết quả"
                    binding.tvSpeciesCount.text = "${allList.size} loài"

                    if (filtered.size < pageSize && allList.size >= currentLimit) {
                        currentLimit += pageSize
                        loadNextPage()
                        return@collectLatest
                    }

                    if (filtered.isEmpty()) {
                        animationHandler.fadeOut(binding.rvHistory)
                        hasMoreData = false
                    } else {
                        animationHandler.fadeIn(binding.rvHistory)
                        animationHandler.fadeOut(binding.emptyStateContainer)
                        hasMoreData = allList.size >= currentLimit

                        val uiModels = withContext(Dispatchers.Default) {
                            filtered.mapIndexed { index, entry ->
                                val isFirst = index == 0 || !isSameDay(entry.timestamp, filtered[index - 1].timestamp)
                                val isLast = index == filtered.size - 1 || !isSameDay(entry.timestamp, filtered[index + 1].timestamp)
                                HistoryUiModel(entry, isFirst, isLast)
                            }
                        }
                        adapter.submitList(uiModels)
                    }

                    isLoadingMore = false
                    adapter.setLoading(false)
                }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val d1 = Instant.ofEpochMilli(t1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(t2).atZone(ZoneId.systemDefault()).toLocalDate()
        return d1 == d2
    }

    private fun loadNextPage() {
        isLoadingMore = true
        adapter.setLoading(true)
        currentLimit += pageSize
        observeHistory()
    }

    private fun toggleSortOption() {
        currentSortOption = if (currentSortOption == HistorySortOption.NEWEST_FIRST)
            HistorySortOption.OLDEST_FIRST else HistorySortOption.NEWEST_FIRST
        updateSortUI()
        currentLimit = pageSize
        observeHistory()
    }

    private fun updateSortUI() {
        binding.tvSortLabel.text = if (currentSortOption == HistorySortOption.NEWEST_FIRST)
            getString(R.string.sort_newest_first) else getString(R.string.sort_oldest_first)
    }

    private fun showDateRangePickerDialog() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.select_date)
            .setTheme(R.style.CustomMaterialDatePickerTheme)
            .setSelection(Pair(
                filterStartDate ?: MaterialDatePicker.todayInUtcMilliseconds(),
                filterEndDate ?: MaterialDatePicker.todayInUtcMilliseconds()
            ))
            .build()
        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        picker.addOnPositiveButtonClickListener { applyDateFilter(it) }
    }

    private fun applyDateFilter(selection: Pair<Long, Long>) {
        val offset = TimeZone.getDefault().getOffset(selection.first)
        filterStartDate = selection.first - offset
        filterEndDate = (selection.second - offset) + 86400000L - 1L

        val start = Instant.ofEpochMilli(filterStartDate!!).atZone(ZoneId.systemDefault())
        val end = Instant.ofEpochMilli(filterEndDate!!).atZone(ZoneId.systemDefault())
        binding.btnFilterByDate.text = "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
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
        val fragment = HistoryDetailFragment().apply {
            arguments = Bundle().apply { putString("HISTORY_ENTRY_JSON", Gson().toJson(entry)) }
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.hold, R.anim.hold, R.anim.slide_out_bottom)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack("Detail")
            .commit()
    }
}