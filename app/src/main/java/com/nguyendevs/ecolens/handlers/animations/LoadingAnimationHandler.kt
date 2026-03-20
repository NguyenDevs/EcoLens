package com.nguyendevs.ecolens.handlers.animations

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

/** Tạo hiệu ứng loading text với dấu chấm (...) nhấp nháy. */
class LoadingAnimationHandler(
    private val tvLoading: TextView,
    private val coroutineScope: CoroutineScope
) {

    private var loadingTextJob: Job? = null
    private var currentTextResId: Int = R.string.analyzing_text
    private val transparentSpan = ForegroundColorSpan(Color.TRANSPARENT)

    /** Cập nhật text resource và restart animation nếu đang chạy. */
    fun setText(resId: Int) {
        if (currentTextResId != resId) {
            currentTextResId = resId
            if (loadingTextJob?.isActive == true) {
                stop()
                start()
            }
        }
    }

    /** Bắt đầu animation nhấp nháy dấu chấm. */
    fun start() {
        if (loadingTextJob?.isActive == true) return

        loadingTextJob = coroutineScope.launch {
            val baseText = tvLoading.context.getString(currentTextResId)
            val fullText = "$baseText..."
            val spannable = SpannableString(fullText)

            var loopCount = 0
            while (isActive) {
                animateDots(spannable, fullText, loopCount)
                loopCount++
                delay(500)
            }
        }
    }

    /** Dừng animation và hủy coroutine job. */
    fun stop() {
        loadingTextJob?.cancel()
        loadingTextJob = null
    }

    /** Ẩn một số dấu chấm theo thứ tự vòng lặp để tạo hiệu ứng nhấp nháy. */
    private fun animateDots(spannable: SpannableString, fullText: String, loopCount: Int) {
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
    }
}