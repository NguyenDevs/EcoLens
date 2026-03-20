package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.transition.TransitionManager
import android.transition.AutoTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fragment hiển thị lịch sử nhận diện, hỗ trợ lọc, sắp xếp, tìm kiếm và phân trang. */
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
    private var currentViewMode = HistoryViewMode.LIST

    private var currentLimit = 10
    private val pageSize = 10
    private var isLoadingMore = false
    private var hasMoreData = true
    private var observeJob: Job? = null

    enum class CategoryFilter { ALL, ANIMALS, PLANTS, FUNGI }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animationHandler = HistoryAnimationHandler(requireContext())
    }

    /** Inflate layout của fragment. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ScreenSpeciesHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Khởi tạo adapter, search, click listeners, và observer. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupSearch()
        setupClickListeners()
        applyViewMode(currentViewMode)
        currentLimit = pageSize
        observeHistory()
        observeTotalCount()
        updateSortUI()
        updateCategoryChipsUI(currentCategory)
        observeLoadingState()
    }

    /** Observe tổng số loài và cập nhật text đếm. */
    private fun observeTotalCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.totalHistoryCount,
                viewModel.isHistoryLoading
            ) { count, isLoading ->
                if (count == 0 && isLoading) {
                    binding.tvSpeciesCount.text = getString(R.string.history_syncing)
                } else {
                    binding.tvSpeciesCount.text = getString(R.string.history_total_species_count, count)
                }
            }.collect { }
        }
    }

    /** Observe trạng thái loading và toggle shimmer. */
    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isHistoryLoading.collectLatest { isLoading ->
                if (isLoading) {
                    binding.shimmerViewContainer.startShimmer()
                    binding.shimmerViewContainer.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.GONE
                    binding.btnViewList.isEnabled = false
                    binding.btnViewGrid.isEnabled = false
                    binding.btnViewList.alpha = 0.5f
                    binding.btnViewGrid.alpha = 0.5f
                } else {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.btnViewList.isEnabled = true
                    binding.btnViewGrid.isEnabled = true
                    binding.btnViewList.alpha = 1.0f
                    binding.btnViewGrid.alpha = 1.0f
                    observeHistory()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Khởi tạo adapter lịch sử với Markwon renderer và scroll listener phân trang. */
    private fun setupAdapter() {
        val markwon = Markwon.builder(requireContext()).usePlugin(HtmlPlugin.create()).build()
        adapter = HistoryAdapter(markwon = markwon, clickListener = { entry ->
            animationHandler.performConfirmFeedback(binding.rvHistory)
            navigateToDetail(entry)
        })
        binding.rvHistory.adapter = adapter
        binding.rvHistory.itemAnimator = null
        binding.rvHistory.setItemViewCacheSize(20)
        binding.rvHistory.setHasFixedSize(true)

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

    /** Thiết lập thanh tìm kiếm với debounce khi nhập text. */
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
            hideKeyboard()
            binding.root.postDelayed({
                toggleSearch(false)
            }, 500)
        }
    }

    /** Ẩn bàn phím ảo. */
    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    /** Thiết lập listener cho chip filter, nút sắp xếp, lọc ngày và toggle view mode. */
    private fun setupClickListeners() {
        binding.chipAll.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            val nextCategory = when (currentCategory) {
                CategoryFilter.ALL -> CategoryFilter.ANIMALS
                CategoryFilter.ANIMALS -> CategoryFilter.PLANTS
                CategoryFilter.PLANTS -> CategoryFilter.FUNGI
                CategoryFilter.FUNGI -> CategoryFilter.ALL
            }
            updateCategoryFilter(nextCategory)
        }
        binding.btnSort.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showFilterBottomSheet()
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
        binding.searchBarContainer.findViewById<View>(R.id.btnSearchIcon).setOnClickListener { view: View ->
            animationHandler.performConfirmFeedback(view)
            if (binding.etSearch.visibility == View.GONE) {
                toggleSearch(true)
            } else {
                if (binding.etSearch.text.isNullOrEmpty()) {
                    toggleSearch(false)
                } else {
                    hideKeyboard()
                }
            }
        }
    }

    /** Animation mở rộng/thu hẹp thanh tìm kiếm. */
    private fun toggleSearch(expand: Boolean) {
        val collapsedWidth = (48 * resources.displayMetrics.density).toInt()
        val expandedWidth = binding.stickyHeader.width - (12 * 2 * resources.displayMetrics.density).toInt()

        val widthAnimator = if (expand) {
            ValueAnimator.ofInt(binding.searchBarContainer.width, expandedWidth)
        } else {
            ValueAnimator.ofInt(binding.searchBarContainer.width, collapsedWidth)
        }

        widthAnimator.duration = if (expand) 320 else 450
        widthAnimator.interpolator = AccelerateDecelerateInterpolator()
        widthAnimator.addUpdateListener { animator: ValueAnimator ->
            val params = binding.searchBarContainer.layoutParams
            params.width = animator.animatedValue as Int
            binding.searchBarContainer.layoutParams = params
        }

        if (expand) {
            binding.titleRow.animate().alpha(0f).setDuration(200).withEndAction {
                binding.titleRow.visibility = View.INVISIBLE
            }.start()

            widthAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    binding.etSearch.visibility = View.VISIBLE
                    binding.ivSearchClear.visibility = View.VISIBLE
                    binding.etSearch.alpha = 0f
                    binding.ivSearchClear.alpha = 0f
                    binding.etSearch.animate().alpha(1f).setDuration(200).setStartDelay(100).start()
                    binding.ivSearchClear.animate().alpha(0.6f).setDuration(200).setStartDelay(100).start()
                }
                override fun onAnimationEnd(animation: Animator) {
                    binding.etSearch.requestFocus()
                    showKeyboard()
                }
            })
        } else {
            binding.etSearch.animate().alpha(0f).setDuration(150).start()
            binding.ivSearchClear.animate().alpha(0f).setDuration(150).start()

            widthAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    binding.titleRow.visibility = View.VISIBLE
                    binding.titleRow.alpha = 0f
                    binding.titleRow.animate().alpha(1f).setDuration(250).start()
                }
                override fun onAnimationEnd(animation: Animator) {
                    binding.etSearch.visibility = View.GONE
                    binding.ivSearchClear.visibility = View.GONE
                    binding.root.postDelayed({
                        binding.etSearch.text?.clear()
                    }, 500)
                }
            })
            hideKeyboard()
        }
        widthAnimator.start()
    }

    /** Hiển thị bàn phím ảo. */
    private fun showKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Áp dụng chế độ xem list hoặc grid và cập nhật LayoutManager. */
    private fun applyViewMode(mode: HistoryViewMode) {
        val previousMode = adapter.viewMode
        adapter.viewMode = mode
        if (mode == HistoryViewMode.GRID) {
            binding.rvHistory.layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (adapter.getItemViewType(position) == 2) 2
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

        if (previousMode != mode) {
            observeHistory()
        }
    }

    /** Cập nhật bộ lọc category hiện tại. */
    private fun updateCategoryFilter(category: CategoryFilter) {
        currentCategory = category
        updateCategoryChipsUI(category)
        currentLimit = pageSize
        observeHistory()
    }

    /** Cập nhật text của chip filter theo category đang chọn. */
    private fun updateCategoryChipsUI(selected: CategoryFilter) {
        val textRes = when (selected) {
            CategoryFilter.ALL -> R.string.history_chipAll
            CategoryFilter.ANIMALS -> R.string.history_chipAnimals
            CategoryFilter.PLANTS -> R.string.history_chipPlants
            CategoryFilter.FUNGI -> R.string.history_chipFungi
        }

        binding.chipAll.apply {
            text = getString(textRes)
            if (selected == CategoryFilter.ALL) {
                setChipIconResource(R.drawable.ic_filter_first)
            } else {
                chipIcon = null
            }
            animationHandler.updateChipStyle(this, true)
        }
    }

    /** Observe và lọc danh sách lịch sử theo category, search query và sort option. */
    private fun observeHistory() {
        observeJob?.cancel()
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate, currentLimit)
                .collectLatest { allList ->
                    val filtered = withContext(Dispatchers.Default) {
                        var list = when (currentCategory) {
                            CategoryFilter.ALL -> allList
                            CategoryFilter.ANIMALS -> allList.filter {
                                val k = it.speciesInfo.kingdom.lowercase()
                                k.contains("animal") || k.contains("động vật")
                            }
                            CategoryFilter.PLANTS -> allList.filter {
                                val k = it.speciesInfo.kingdom.lowercase()
                                k.contains("plant") || k.contains("thực vật")
                            }
                            CategoryFilter.FUNGI -> allList.filter {
                                val k = it.speciesInfo.kingdom.lowercase()
                                k.contains("fungi") || k.contains("nấm")
                            }
                        }

                        if (searchQuery.isNotEmpty()) {
                            list = list.filter {
                                it.speciesInfo.commonName.contains(searchQuery, ignoreCase = true) ||
                                        it.speciesInfo.scientificName.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        when (currentSortOption) {
                            HistorySortOption.ALPHABETICAL -> list.sortedBy { it.speciesInfo.commonName }
                            HistorySortOption.CONFIDENCE_HIGH -> list.sortedByDescending { it.speciesInfo.confidence }
                            else -> list
                        }
                    }

                    val isFiltering = searchQuery.isNotEmpty() || currentCategory != CategoryFilter.ALL || filterStartDate != null

                    binding.tvResultCount.visibility = if (isFiltering) View.VISIBLE else View.INVISIBLE
                    binding.tvResultCount.text = getString(R.string.history_results_count, filtered.size)

                    if (filtered.size < pageSize && allList.size >= currentLimit) {
                        currentLimit += pageSize
                        loadNextPage()
                        return@collectLatest
                    }

                    if (filtered.isEmpty()) {
                        if (viewModel.isHistoryLoading.value) {
                            return@collectLatest
                        }
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.visibility = View.GONE
                        animationHandler.fadeOut(binding.rvHistory)
                        animationHandler.fadeIn(binding.emptyStateContainer)
                        hasMoreData = false
                    } else {
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.visibility = View.GONE
                        if (binding.rvHistory.visibility != View.VISIBLE || binding.rvHistory.alpha < 1f) {
                            animationHandler.fadeIn(binding.rvHistory)
                        }
                        animationHandler.fadeOut(binding.emptyStateContainer)
                        hasMoreData = allList.size >= currentLimit

                        val uiModels = withContext(Dispatchers.Default) {
                            val models = mutableListOf<HistoryUiModel>()
                            var i = 0
                            while (i < filtered.size) {
                                val entry = filtered[i]
                                val isFirst = i == 0 || !isSameDay(entry.timestamp, filtered[i - 1].timestamp)
                                val isLast = i == filtered.size - 1 || !isSameDay(entry.timestamp, filtered[i + 1].timestamp)

                                models.add(HistoryUiModel(entry, isFirst, isLast))
                                if (currentViewMode == HistoryViewMode.GRID && isLast) {
                                    var dayStartIndex = i
                                    while (dayStartIndex > 0 && isSameDay(filtered[dayStartIndex].timestamp, filtered[dayStartIndex - 1].timestamp)) {
                                        dayStartIndex--
                                    }
                                    val itemsInDay = i - dayStartIndex + 1
                                    if (itemsInDay % 2 != 0) {
                                        models.add(HistoryUiModel(entry, isFirstOfDay = false, isLastOfDay = true, isPlaceholder = true))
                                    }
                                }
                                i++
                            }
                            models
                        }
                        adapter.submitList(uiModels)
                    }

                    isLoadingMore = false
                    adapter.setLoading(false)
                }
        }
    }

    /** Kiểm tra hai timestamp có cùng ngày không. */
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val d1 = Instant.ofEpochMilli(t1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(t2).atZone(ZoneId.systemDefault()).toLocalDate()
        return d1 == d2
    }

    /** Tải trang tiếp theo của danh sách lịch sử. */
    private fun loadNextPage() {
        isLoadingMore = true
        adapter.setLoading(true)
        currentLimit += pageSize
        observeHistory()
    }

    /** Cập nhật nhãn sắp xếp hiện tại. */
    private fun updateSortUI() {
        binding.tvSortLabel.text = when (currentSortOption) {
            HistorySortOption.NEWEST_FIRST -> getString(R.string.sort_newest_first)
            HistorySortOption.OLDEST_FIRST -> getString(R.string.sort_oldest_first)
            HistorySortOption.ALPHABETICAL -> getString(R.string.sort_az)
            HistorySortOption.CONFIDENCE_HIGH -> getString(R.string.sort_confidence)
            HistorySortOption.FAVORITE -> getString(R.string.sort_favorite)
        }
    }

    /** Hiển thị bottom sheet chọn thứ tự sắp xếp. */
    private fun showFilterBottomSheet() {
        val sheet = FilterHistoryBottomSheet.newInstance(currentSortOption)
        sheet.onApplyListener = { sort ->
            currentSortOption = sort
            updateSortUI()
            currentLimit = pageSize
            observeHistory()
        }
        sheet.show(parentFragmentManager, "FILTER_SHEET")
    }

    /** Cập nhật UI chip filter ngày theo range đang chọn. */
    private fun updateDateFilterUI() {
        if (filterStartDate != null || filterEndDate != null) {
            val start = filterStartDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            val end = filterEndDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }

            binding.btnFilterByDate.text = when {
                start != null && end != null -> "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
                start != null -> "Từ ${dateFormatter.format(start)}"
                end != null -> "Đến ${dateFormatter.format(end)}"
                else -> getString(R.string.select_date)
            }
            binding.btnFilterByDate.isCloseIconVisible = true
            animationHandler.updateChipStyle(binding.btnFilterByDate, true)
        } else {
            clearDateFilter()
        }
    }

    /** Hiển thị date range picker để lọc theo ngày. */
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

    /** Áp dụng bộ lọc theo khoảng ngày được chọn. */
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

    /** Xóa bộ lọc ngày và reset về mặc định. */
    private fun clearDateFilter() {
        filterStartDate = null
        filterEndDate = null
        binding.btnFilterByDate.text = getString(R.string.select_date)
        binding.btnFilterByDate.isCloseIconVisible = false
        animationHandler.updateChipStyle(binding.btnFilterByDate, false)
        currentLimit = pageSize
        observeHistory()
    }

    /** Điều hướng đến màn hình chi tiết lịch sử. */
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