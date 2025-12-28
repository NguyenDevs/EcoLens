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

    fun setup() {
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
            val animator = ValueAnimator.ofInt(collapsedWidthPx, expandedWidthPx)
            animator.duration = 320
            animator.addUpdateListener { animation ->
                val params = searchBarContainer.layoutParams
                params.width = animation.animatedValue as Int
                searchBarContainer.layoutParams = params
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    textInputLayoutSearch.visibility = View.VISIBLE
                    etSearchQuery.setText(text)
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    etSearchQuery.requestFocus()
                    etSearchQuery.setSelection(etSearchQuery.text.length)
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT)
                }
            })
            animator.start()
            isSearchBarExpanded = true
        } else {
            etSearchQuery.setText(text)
            etSearchQuery.post {
                etSearchQuery.requestFocus()
                etSearchQuery.setSelection(text.length)
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun collapseSearchBar() {
        if (isSearchBarExpanded) {
            val animator = ValueAnimator.ofInt(expandedWidthPx, collapsedWidthPx)
            animator.duration = 320
            animator.addUpdateListener { animation ->
                val params = searchBarContainer.layoutParams
                params.width = animation.animatedValue as Int
                searchBarContainer.layoutParams = params
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    textInputLayoutSearch.visibility = View.GONE
                    etSearchQuery.text?.clear()
                }
            })
            animator.start()
            isSearchBarExpanded = false
        }
    }

    fun isExpanded() = isSearchBarExpanded

    private fun performGoogleSearch() {
        val query = etSearchQuery.text.toString().trim()
        if (query.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.error_browser), Toast.LENGTH_SHORT).show()
            }
        } else {
            collapseSearchBar()
        }
    }
}