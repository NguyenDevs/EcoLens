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

/**
 * Handler quản lý search bar có thể expand/collapse
 * Hỗ trợ Google search với animation mượt mà
 */
class SearchBarHandler(
    private val context: Context,
    private val searchBarContainer: MaterialCardView,
    private val textInputLayoutSearch: TextInputLayout,
    private val etSearchQuery: EditText,
    private val btnSearchAction: ImageView
) {

    private val collapsedWidthPx = (50 * context.resources.displayMetrics.density).toInt()
    private val expandedWidthPx = (330 * context.resources.displayMetrics.density).toInt()

    private var isSearchBarExpanded = false
    private val viewsToHide = mutableListOf<View>()

    init {
        setupClickListeners()
    }

    /**
     * Set các views cần ẩn đi khi search bar expand
     */
    fun setViewsToHide(views: List<View>) {
        viewsToHide.clear()
        viewsToHide.addAll(views)
    }

    // ==================== SETUP ====================

    /**
     * Cấu hình click listeners cho search button và IME action
     */
    private fun setupClickListeners() {
        btnSearchAction.setOnClickListener {
            btnSearchAction.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            if (!isSearchBarExpanded) {
                expandSearchBar("")
            } else {
                performGoogleSearch()
            }
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

    // ==================== PUBLIC METHODS ====================

    /**
     * Expand search bar với text tùy chọn
     * Tự động focus và hiển thị bàn phím
     */
    fun expandSearchBar(text: String = "") {
        if (!isSearchBarExpanded) {
            animateWidth(
                from = collapsedWidthPx,
                to = expandedWidthPx,
                onStart = {
                    textInputLayoutSearch.visibility = View.VISIBLE
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

    /**
     * Collapse search bar
     * Tự động clear text và ẩn bàn phím
     */
    fun collapseSearchBar() {
        if (isSearchBarExpanded) {
            animateWidth(
                from = expandedWidthPx,
                to = collapsedWidthPx,
                onEnd = {
                    textInputLayoutSearch.visibility = View.GONE
                    etSearchQuery.text?.clear()
                    hideKeyboard()
                }
            )
            isSearchBarExpanded = false
        }
    }

    /**
     * Kiểm tra xem search bar có đang expanded không
     */
    fun isExpanded() = isSearchBarExpanded

    // ==================== PRIVATE METHODS ====================

    /**
     * Thực hiện Google search với query từ EditText
     * Collapse search bar nếu query rỗng
     */
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

    /**
     * Animate width của search bar container
     */
    private fun animateWidth(
        from: Int,
        to: Int,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        ValueAnimator.ofInt(from, to).apply {
            duration = 320
            addUpdateListener { animation ->
                val params = searchBarContainer.layoutParams
                params.width = animation.animatedValue as Int
                searchBarContainer.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    onStart?.invoke()
                    if (to > from) { // Expanding
                        viewsToHide.forEach {
                            it.animate().alpha(0f).setDuration(200).withEndAction { 
                                it.visibility = View.INVISIBLE 
                            }.start()
                        }
                    } else { // Collapsing
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

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearchQuery.windowToken, 0)
    }
}