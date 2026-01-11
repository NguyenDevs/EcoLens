package com.nguyendevs.ecolens.handlers

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R

class ShimmerEffectHandler(private val context: Context) {
    private var taxonomyShimmerAnimator: ValueAnimator? = null

    fun startTaxonomyShimmer(view: View?) {
        if (view == null || taxonomyShimmerAnimator != null) return

        taxonomyShimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val width = view.width.toFloat()
                val height = view.height.toFloat()

                if (width <= 0 || height <= 0) return@addUpdateListener

                val diagonal = Math.sqrt((width * width + height * height).toDouble()).toFloat()
                val shimmerWidth = diagonal * 0.5f
                val offset = diagonal * (progress - 0.3f)

                val isDarkMode =
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val backgroundColor: Int
                val transparent: Int
                val fadeIn1: Int
                val fadeIn2: Int
                val shimmerColor: Int
                val fadeOut2: Int
                val fadeOut1: Int

                if (isDarkMode) {
                    backgroundColor = Color.parseColor("#2C2C2C")
                    transparent = Color.parseColor("#002C2C2C")
                    fadeIn1 = Color.parseColor("#20454545")
                    fadeIn2 = Color.parseColor("#60454545")
                    shimmerColor = Color.parseColor("#FF454545")
                    fadeOut2 = Color.parseColor("#60454545")
                    fadeOut1 = Color.parseColor("#20454545")
                } else {
                    backgroundColor = Color.parseColor("#ECEFF1")
                    transparent = Color.parseColor("#00ECEFF1")
                    fadeIn1 = Color.parseColor("#40F5F7F9")
                    fadeIn2 = Color.parseColor("#80F8F9FB")
                    shimmerColor = Color.parseColor("#FFFAFBFC")
                    fadeOut2 = Color.parseColor("#80F8F9FB")
                    fadeOut1 = Color.parseColor("#40F5F7F9")
                }

                val gradient = LinearGradient(
                    offset, offset,
                    offset + shimmerWidth, offset + shimmerWidth,
                    intArrayOf(transparent, fadeIn1, fadeIn2, shimmerColor, fadeOut2, fadeOut1, transparent),
                    floatArrayOf(0f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 1f),
                    Shader.TileMode.CLAMP
                )

                val paint = Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                    isDither = true
                }

                val bgPaint = Paint().apply {
                    color = backgroundColor
                    isAntiAlias = true
                }

                val shapeDrawable = object : ShapeDrawable(RectShape()) {
                    override fun onDraw(shape: Shape, canvas: Canvas, p: Paint) {
                        val cornerRadius = 20f.dpToPx()
                        val path = Path().apply {
                            addRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawRect(0f, 0f, width, height, bgPaint)
                        canvas.drawRect(0f, 0f, width, height, paint)
                        canvas.restore()
                    }
                }

                view.background = shapeDrawable
                view.invalidate()
            }
            start()
        }
    }

    fun stopTaxonomyShimmer(view: View) {
        taxonomyShimmerAnimator?.cancel()
        taxonomyShimmerAnimator = null
        view.setBackgroundResource(R.drawable.bg_white_rounded)
        view.backgroundTintList = ContextCompat.getColorStateList(context, R.color.surface_variant)
    }

    fun destroy() {
        taxonomyShimmerAnimator?.cancel()
        taxonomyShimmerAnimator = null
    }

    private fun Float.dpToPx(): Float = this * context.resources.displayMetrics.density
}