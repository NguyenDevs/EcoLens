package com.nguyendevs.ecolens.handlers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.nguyendevs.ecolens.R

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

    init {
        btnSearchAction.setOnClickListener {
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

    fun isExpanded() = isSearchBarExpanded

    private fun performGoogleSearch() {
        val query = etSearchQuery.text.toString().trim()
        if (query.isNotEmpty()) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                context.startActivity(intent)
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        } else {
            collapseSearchBar()
        }
    }

    private fun animateWidth(from: Int, to: Int, onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null) {
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