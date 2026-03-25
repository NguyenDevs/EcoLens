package com.nguyendevs.ecolens.handlers.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.nguyendevs.ecolens.R

/** Handler quản lý thanh tìm kiếm có thể mở rộng/thu gọn, hỗ trợ Google search. */
class SearchBarHandler(
    private val context: Context,
    private val searchBarContainer: MaterialCardView,
    private val textInputLayoutSearch: TextInputLayout,
    private val etSearchQuery: EditText,
    private val btnSearchAction: ImageView,
    private val ivSearchClear: ImageView
) {

    private val collapsedWidthPx = (50 * context.resources.displayMetrics.density).toInt()
    private val expandedWidthPx = (330 * context.resources.displayMetrics.density).toInt()

    private var isSearchBarExpanded = false
    private val viewsToHide = mutableListOf<View>()

    init {
        setupClickListeners()
    }

    /** Đăng ký danh sách view cần ẩn khi thanh tìm kiếm mở rộng. */
    fun setViewsToHide(views: List<View>) {
        viewsToHide.clear()
        viewsToHide.addAll(views)
    }

    /** Thiết lập listener cho nút tìm kiếm và phím IME Action. */
    private fun setupClickListeners() {
        btnSearchAction.setOnClickListener {
            btnSearchAction.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            if (!isSearchBarExpanded) {
                expandSearchBar("")
            } else {
                performGoogleSearch()
            }
        }

        ivSearchClear.setOnClickListener {
            collapseSearchBar()
        }

        etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performGoogleSearch()
                true
            } else {
                false
            }
        }
    }

    /** Mở rộng thanh tìm kiếm với text tùy chọn và focus bàn phím. */
    fun expandSearchBar(text: String = "") {
        if (!isSearchBarExpanded) {
            animateWidth(
                from = collapsedWidthPx,
                to = expandedWidthPx,
                onStart = {
                    textInputLayoutSearch.visibility = View.VISIBLE
                    textInputLayoutSearch.alpha = 0f
                    textInputLayoutSearch.animate().alpha(1f).setDuration(250).setStartDelay(50).start()
                    ivSearchClear.visibility = View.VISIBLE
                    ivSearchClear.alpha = 0f
                    ivSearchClear.animate().alpha(0.6f).setDuration(250).setStartDelay(50).start()
                    etSearchQuery.setText(text)
                },
                onEnd = {
                    etSearchQuery.requestFocus()
                    etSearchQuery.setSelection(etSearchQuery.text.length)
                    showKeyboard()
                }
            )
            isSearchBarExpanded = true
        } else {
            etSearchQuery.setText(text)
            etSearchQuery.post {
                etSearchQuery.requestFocus()
                etSearchQuery.setSelection(text.length)
                showKeyboard()
            }
        }
    }

    /** Thu gọn thanh tìm kiếm và xóa text. */
    fun collapseSearchBar() {
        if (isSearchBarExpanded) {
            isSearchBarExpanded = false
            textInputLayoutSearch.animate().alpha(0f).setDuration(200).start()
            ivSearchClear.animate().alpha(0f).setDuration(200).start()
            animateWidth(
                from = expandedWidthPx,
                to = collapsedWidthPx,
                onEnd = {
                    etSearchQuery.text?.clear()
                    textInputLayoutSearch.visibility = View.GONE
                    ivSearchClear.visibility = View.GONE
                    hideKeyboard()
                }
            )
        }
    }

    /** Kiểm tra thanh tìm kiếm có đang mở rộng không. */
    fun isExpanded() = isSearchBarExpanded

    /** Thực hiện Google search với query từ EditText. */
    private fun performGoogleSearch() {
        val query = etSearchQuery.text.toString().trim()
        if (query.isNotEmpty()) {
            runCatching {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=$query")
                )
                context.startActivity(intent)
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_browser),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            collapseSearchBar()
        }
    }

    /** Animation thay đổi chiều rộng của thanh tìm kiếm. */
    private fun animateWidth(
        from: Int,
        to: Int,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        ValueAnimator.ofInt(from, to).apply {
            duration = if (to > from) 320 else 450
            addUpdateListener { animation ->
                val params = searchBarContainer.layoutParams
                params.width = animation.animatedValue as Int
                searchBarContainer.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    onStart?.invoke()
                    if (to > from) {
                        viewsToHide.forEach {
                            it.animate().alpha(0f).setDuration(200).withEndAction {
                                it.visibility = View.INVISIBLE
                            }.start()
                        }
                    } else {
                        viewsToHide.forEach {
                            it.visibility = View.VISIBLE
                            it.animate().alpha(1f).setDuration(200).start()
                        }
                    }
                }
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    /** Hiển thị bàn phím ảo. */
    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Ẩn bàn phím ảo. */
    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearchQuery.windowToken, 0)
    }
}