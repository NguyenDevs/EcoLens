package com.nguyendevs.ecolens.fragments.history

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var observeJob: Job? = null

    enum class CategoryFilter { ALL, ANIMALS, PLANTS, FUNGI, PROTOZOA, CHROMISTA }

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
        applyViewMode(HistoryViewMode.LIST)
        observeHistory()
        observeTotalCount()
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
                    binding.btnViewList.alpha = 0.75f
                    binding.btnViewGrid.alpha = 0.75f
                } else {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.btnViewList.isEnabled = true
                    binding.btnViewGrid.isEnabled = true
                    binding.btnViewList.alpha = 1.0f
                    binding.btnViewGrid.alpha = 1.0f
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Khởi tạo adapter lịch sử và scroll listener phân trang. */
    private fun setupAdapter() {
        adapter = HistoryAdapter(clickListener = { entry ->
            animationHandler.performConfirmFeedback(binding.rvHistory)
            navigateToDetail(entry)
        })
        binding.rvHistory.adapter = adapter
        binding.rvHistory.itemAnimator = null
        binding.rvHistory.setItemViewCacheSize(20)
        binding.rvHistory.setHasFixedSize(false)

        // Pre-inflate pool để tránh tạo ViewHolder lạnh khi scroll lần đầu
        binding.rvHistory.recycledViewPool.setMaxRecycledViews(0, 10) // LIST
        binding.rvHistory.recycledViewPool.setMaxRecycledViews(1, 10) // GRID

        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                adapter.isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
            }

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
                if (total <= lastVisible + 5) {
                    viewModel.updateHistoryFilter(limit = total + 30)
                }
            }
        })
    }

    /** Thiết lập thanh tìm kiếm với debounce khi nhập text. */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val search = s?.toString()?.trim() ?: ""
                binding.ivSearchClear.visibility = if (search.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.updateHistoryFilter(search = search, resetLimit = true)
            }
        })
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }
        binding.ivSearchClear.setOnClickListener {
            toggleSearch(false)
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
            val currentCat = when (viewModel.historyCategory.value) {
                "" -> CategoryFilter.ALL
                "animal" -> CategoryFilter.ANIMALS
                "plant" -> CategoryFilter.PLANTS
                "fungi" -> CategoryFilter.FUNGI
                "protozoa" -> CategoryFilter.PROTOZOA
                "chromista" -> CategoryFilter.CHROMISTA
                else -> CategoryFilter.ALL
            }
            val nextCategory = when (currentCat) {
                CategoryFilter.ALL -> CategoryFilter.ANIMALS
                CategoryFilter.ANIMALS -> CategoryFilter.PLANTS
                CategoryFilter.PLANTS -> CategoryFilter.FUNGI
                CategoryFilter.FUNGI -> CategoryFilter.PROTOZOA
                CategoryFilter.PROTOZOA -> CategoryFilter.CHROMISTA
                CategoryFilter.CHROMISTA -> CategoryFilter.ALL
            }
            updateCategoryFilter(nextCategory)
        }
        binding.btnSort.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showSortDropdown(it)
        }
        binding.btnFilterByDate.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showDateRangePickerDialog()
        }
        binding.btnFilterByDate.setOnCloseIconClickListener { clearDateFilter() }

        binding.btnViewList.setOnClickListener {
            applyViewMode(HistoryViewMode.LIST)
        }
        binding.btnViewGrid.setOnClickListener {
            applyViewMode(HistoryViewMode.GRID)
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
                    binding.etSearch.text?.clear()
                    binding.etSearch.visibility = View.GONE
                    binding.ivSearchClear.visibility = View.GONE
                    hideKeyboard()
                }
            })
        }
        widthAnimator.start()
    }

    /** Hiển thị bàn phím ảo. */
    private fun showKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Áp dụng chế độ xem list hoặc grid và cập nhật LayoutManager kèm khoảng nghỉ. */
    private fun applyViewMode(mode: HistoryViewMode) {
        val previousMode = adapter.viewMode
        updateViewModeButtonsUI(mode)

        // Nếu là lần đầu khởi tạo (LayoutManager chưa có), áp dụng ngay không delay
        if (binding.rvHistory.layoutManager == null) {
            adapter.viewMode = mode
            updateLayoutManager(mode)
            rebuildAndSubmitModels()
            return
        }

        if (previousMode != mode) {
            viewLifecycleOwner.lifecycleScope.launch {
                adapter.submitList(emptyList())
                kotlinx.coroutines.delay(450)
                adapter.viewMode = mode
                updateLayoutManager(mode)
                rebuildAndSubmitModels()
            }
        }
    }

    /** Cập nhật UI cho các nút chuyển đổi chế độ xem. */
    private fun updateViewModeButtonsUI(mode: HistoryViewMode) {
        if (mode == HistoryViewMode.GRID) {
            binding.btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_active)
            binding.btnViewGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnViewList.setBackgroundResource(R.drawable.bg_view_toggle)
            binding.btnViewList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
        } else {
            binding.btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_active)
            binding.btnViewList.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle)
            binding.btnViewGrid.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
        }
    }

    /** Cập nhật LayoutManager cho RecyclerView dựa trên chế độ xem. */
    private fun updateLayoutManager(mode: HistoryViewMode) {
        if (mode == HistoryViewMode.GRID) {
            binding.rvHistory.layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        // ViewType 2 là LOADING, chiếm full width
                        return if (adapter.getItemViewType(position) == 2) 2
                        else 1
                    }
                }
            }
        } else {
            binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /** Cập nhật bộ lọc category hiện tại. */
    private fun updateCategoryFilter(category: CategoryFilter) {
        updateCategoryChipsUI(category)
        val catString = when (category) {
            CategoryFilter.ALL -> ""
            CategoryFilter.ANIMALS -> "animal"
            CategoryFilter.PLANTS -> "plant"
            CategoryFilter.FUNGI -> "fungi"
            CategoryFilter.PROTOZOA -> "protozoa"
            CategoryFilter.CHROMISTA -> "chromista"
        }
        viewModel.updateHistoryFilter(category = catString, resetLimit = true)
    }

    /** Cập nhật text của chip filter theo category đang chọn. */
    private fun updateCategoryChipsUI(selected: CategoryFilter) {
        val textRes = when (selected) {
            CategoryFilter.ALL -> R.string.history_chipAll
            CategoryFilter.ANIMALS -> R.string.history_chipAnimals
            CategoryFilter.PLANTS -> R.string.history_chipPlants
            CategoryFilter.FUNGI -> R.string.history_chipFungi
            CategoryFilter.PROTOZOA -> R.string.history_chipProtozoa
            CategoryFilter.CHROMISTA -> R.string.history_chipChromista
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

    /** Observe dữ liệu lịch sử phản ứng từ ViewModel. */
    private var lastFilteredData: List<HistoryEntry> = emptyList()

    private fun observeHistory() {
        observeJob?.cancel()
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.historyList.collectLatest { filtered ->
                lastFilteredData = filtered
                if (filtered.isEmpty()) {
                    if (viewModel.isHistoryLoading.value) return@collectLatest
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    animationHandler.fadeOut(binding.rvHistory)
                    animationHandler.fadeIn(binding.emptyStateContainer)
                } else {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.btnViewList.isEnabled = true
                    binding.btnViewGrid.isEnabled = true
                    binding.btnViewList.alpha = 1.0f
                    binding.btnViewGrid.alpha = 1.0f
                    if (binding.rvHistory.visibility != View.VISIBLE || binding.rvHistory.alpha < 1f) {
                        animationHandler.fadeIn(binding.rvHistory)
                    }
                    animationHandler.fadeOut(binding.emptyStateContainer)
                    buildAndSubmitModels(filtered)
                }
                adapter.setLoading(false)
            }
        }
    }

    /** Re-build models khi view mode thay đổi. */
    private fun rebuildAndSubmitModels() {
        val data = lastFilteredData
        if (data.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            buildAndSubmitModels(data)
        }
    }

    /** Build UiModels trên background thread và submit. */
    private suspend fun buildAndSubmitModels(filtered: List<HistoryEntry>) {
        val uiModels = withContext(Dispatchers.Default) {
            val models = mutableListOf<HistoryUiModel>()
            var i = 0
            while (i < filtered.size) {
                val entry = filtered[i]
                val isFirst = i == 0 || !isSameDay(entry.timestamp, filtered[i - 1].timestamp)
                val isLast = i == filtered.size - 1 || !isSameDay(entry.timestamp, filtered[i + 1].timestamp)

                models.add(HistoryUiModel(entry, isFirst, isLast))

                // Luôn thêm placeholder cho ngày có số lẻ items (cần cho grid alignment)
                if (isLast) {
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

    /** Kiểm tra hai timestamp có cùng ngày không. */
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val d1 = Instant.ofEpochMilli(t1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(t2).atZone(ZoneId.systemDefault()).toLocalDate()
        return d1 == d2
    }

    /** Cập nhật nhãn sắp xếp hiện tại. */
    private fun updateSortUI(sort: HistorySortOption) {
        binding.tvSortLabel.text = when (sort) {
            HistorySortOption.NEWEST_FIRST -> getString(R.string.sort_newest_first)
            HistorySortOption.OLDEST_FIRST -> getString(R.string.sort_oldest_first)
            HistorySortOption.ALPHABETICAL -> getString(R.string.sort_az)
            HistorySortOption.CONFIDENCE_HIGH -> getString(R.string.sort_confidence)
            HistorySortOption.FAVORITE -> getString(R.string.sort_favorite)
        }
    }

    private fun showSortDropdown(anchor: View) {
        val widthPx = (250 * resources.displayMetrics.density).toInt()
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_sort_history, null)

        val popup = PopupWindow(popupView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            exitTransition = android.transition.Fade(android.transition.Fade.OUT).apply {
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
            }
        }

        updateDropdownUI(popupView, viewModel.historySortOption.value)

        val rowOptions = listOf(
            R.id.itemNewest    to HistorySortOption.NEWEST_FIRST,
            R.id.itemOldest    to HistorySortOption.OLDEST_FIRST,
            R.id.itemAlpha     to HistorySortOption.ALPHABETICAL,
            R.id.itemConfidence to HistorySortOption.CONFIDENCE_HIGH,
            R.id.itemFavorite  to HistorySortOption.FAVORITE
        )
        val rowViews = rowOptions.map { (id, _) -> popupView.findViewById<ViewGroup>(id) }

        rowViews.forEach { row ->
            row.alpha = 0f
            row.translationY = -12f
        }

        for ((rowId, sortOption) in rowOptions) {
            popupView.findViewById<View>(rowId).setOnClickListener {
                if (sortOption == viewModel.historySortOption.value) {
                    dismissWithFade(popup)
                    return@setOnClickListener
                }

                updateSortUI(sortOption)
                popup.setOnDismissListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        adapter.submitList(emptyList())
                        kotlinx.coroutines.delay(450)
                        viewModel.updateHistoryFilter(sort = sortOption, resetLimit = true)
                    }
                }
                dismissWithFade(popup)
            }
        }

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val xOffset = anchorLoc[0] + anchor.width - widthPx
        val yOffset = anchorLoc[1] + anchor.height + (6 * resources.displayMetrics.density).toInt()

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)

        popupView.alpha = 0f
        popupView.scaleX = 0.90f
        popupView.scaleY = 0.90f
        popupView.pivotX = widthPx.toFloat()
        popupView.pivotY = 0f
        popupView.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(320)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        rowViews.forEachIndexed { index, row ->
            row.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(80L + index * 70L)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun dismissWithFade(popup: PopupWindow) {
        popup.dismiss()
    }

    private fun updateDropdownUI(root: View, selected: HistorySortOption) {
        data class RowIds(val rowId: Int, val labelId: Int, val option: HistorySortOption)
        val rows = listOf(
            RowIds(R.id.itemNewest,    R.id.labelNewest,    HistorySortOption.NEWEST_FIRST),
            RowIds(R.id.itemOldest,    R.id.labelOldest,    HistorySortOption.OLDEST_FIRST),
            RowIds(R.id.itemAlpha,     R.id.labelAlpha,     HistorySortOption.ALPHABETICAL),
            RowIds(R.id.itemConfidence, R.id.labelConfidence, HistorySortOption.CONFIDENCE_HIGH),
            RowIds(R.id.itemFavorite,  R.id.labelFavorite,  HistorySortOption.FAVORITE)
        )
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        for (r in rows) {
            val rowView = root.findViewById<ViewGroup>(r.rowId)
            val iconView = rowView.getChildAt(0) as ImageView
            val labelView = root.findViewById<TextView>(r.labelId)
            
            if (r.option == selected) {
                iconView.setColorFilter(primaryColor)
                labelView.setTextColor(primaryColor)
                labelView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                iconView.clearColorFilter()
                labelView.setTextColor(textPrimaryColor)
                labelView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    /** Cập nhật UI chip filter ngày theo range đang chọn. */
    private fun updateDateFilterUI() {
        val startMilli = viewModel.historyStartDate.value
        val endMilli = viewModel.historyEndDate.value

        if (startMilli != null || endMilli != null) {
            val start = Instant.ofEpochMilli(startMilli ?: 0L).atZone(ZoneId.systemDefault())
            val end = Instant.ofEpochMilli(endMilli ?: Long.MAX_VALUE).atZone(ZoneId.systemDefault())

            binding.btnFilterByDate.text = "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
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
            .build()
        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        picker.addOnPositiveButtonClickListener { applyDateFilter(it) }
    }

    /** Áp dụng bộ lọc theo khoảng ngày được chọn. */
    private fun applyDateFilter(selection: Pair<Long, Long>) {
        val offset = TimeZone.getDefault().getOffset(selection.first)
        val startAt = selection.first - offset
        val endAt = (selection.second - offset) + 86400000L - 1L

        val s = Instant.ofEpochMilli(startAt).atZone(ZoneId.systemDefault())
        val e = Instant.ofEpochMilli(endAt).atZone(ZoneId.systemDefault())
        binding.btnFilterByDate.text = "${dateFormatter.format(s)} - ${dateFormatter.format(e)}"
        binding.btnFilterByDate.isCloseIconVisible = true
        animationHandler.updateChipStyle(binding.btnFilterByDate, true)
        viewModel.updateHistoryFilter(start = startAt, end = endAt, resetLimit = true)
    }

    /** Xóa bộ lọc ngày và reset về mặc định. */
    private fun clearDateFilter() {
        binding.btnFilterByDate.text = getString(R.string.select_date)
        binding.btnFilterByDate.isCloseIconVisible = false
        animationHandler.updateChipStyle(binding.btnFilterByDate, false)
        viewModel.updateHistoryFilter(start = 0L, end = Long.MAX_VALUE, resetLimit = true)
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