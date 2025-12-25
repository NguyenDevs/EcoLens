package com.nguyendevs.ecolens.handlers

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LoadingAnimationHandler(
    private val tvLoading: TextView,
    private val coroutineScope: CoroutineScope
) {

    private var loadingTextJob: Job? = null

    fun start() {
        if (loadingTextJob?.isActive == true) return
        loadingTextJob = coroutineScope.launch {
            val baseText = tvLoading.context.getString(com.nguyendevs.ecolens.R.string.analyzing_text)
            val dots = "..."
            val fullText = "$baseText$dots"

            var loopCount = 0
            while (isActive) {
                val spannable = SpannableString(fullText)
                val visibleDots = (loopCount % 3) + 1
                val hideCount = 3 - visibleDots

                if (hideCount > 0) {
                    val start = fullText.length - hideCount
                    val end = fullText.length

                    spannable.setSpan(
                        ForegroundColorSpan(Color.TRANSPARENT),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                tvLoading.text = spannable
                loopCount++
                delay(500)
            }
        }
    }

    fun stop() {
        loadingTextJob?.cancel()
        loadingTextJob = null
    }
}