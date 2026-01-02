package com.nguyendevs.ecolens.handlers

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.nguyendevs.ecolens.R
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
    private var currentTextResId: Int = R.string.analyzing_text

    // Tối ưu: Khởi tạo Span một lần để tái sử dụng
    private val transparentSpan = ForegroundColorSpan(Color.TRANSPARENT)

    fun setText(resId: Int) {
        if (currentTextResId != resId) {
            currentTextResId = resId
            if (loadingTextJob?.isActive == true) {
                stop()
                start()
            }
        }
    }

    fun start() {
        if (loadingTextJob?.isActive == true) return
        loadingTextJob = coroutineScope.launch {
            val baseText = tvLoading.context.getString(currentTextResId)
            val fullText = "$baseText..."
            // Tối ưu: Tạo SpannableString một lần duy nhất
            val spannable = SpannableString(fullText)

            var loopCount = 0
            while (isActive) {
                // Reset span cũ trước khi set vị trí mới
                spannable.removeSpan(transparentSpan)

                val visibleDots = (loopCount % 3) + 1
                val hideCount = 3 - visibleDots

                if (hideCount > 0) {
                    val start = fullText.length - hideCount
                    val end = fullText.length
                    spannable.setSpan(
                        transparentSpan,
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