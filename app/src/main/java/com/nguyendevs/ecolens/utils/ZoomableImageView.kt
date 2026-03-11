package com.nguyendevs.ecolens.utils

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr),
    View.OnTouchListener,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    private var mMatrix: Matrix = Matrix()
    private val mMatrixValues = FloatArray(9)
    private var mMode = NONE

    // Scales
    private var mSaveScale = 1f
    private var mMinScale = 1f
    private var mMaxScale = 3f

    // View dimensions
    private var origWidth = 0f
    private var origHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var last = PointF()
    private var start = PointF()

    private var mScaleDetector: ScaleGestureDetector
    private var mGestureDetector: GestureDetector

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }

    init {
        super.setClickable(true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, this)
        mMatrix = Matrix()
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if ((mSaveScale == 1f) && drawable != null) {
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight

            val scaleX = viewWidth.toFloat() / drawableWidth.toFloat()
            val scaleY = viewHeight.toFloat() / drawableHeight.toFloat()
            val scale = scaleX.coerceAtMost(scaleY)

            mMatrix.setScale(scale, scale)

            // Center the image
            var redundantYSpace = viewHeight.toFloat() - (scale * drawableHeight.toFloat())
            var redundantXSpace = viewWidth.toFloat() - (scale * drawableWidth.toFloat())

            redundantYSpace /= 2f
            redundantXSpace /= 2f

            mMatrix.postTranslate(redundantXSpace, redundantYSpace)
            
            origWidth = viewWidth - 2 * redundantXSpace
            origHeight = viewHeight - 2 * redundantYSpace

            imageMatrix = mMatrix
        }
        fixTranslation()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)

        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
                mMode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mMode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * mSaveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * mSaveScale)
                    mMatrix.postTranslate(fixTransX, fixTransY)
                    fixTranslation()
                    last.set(curr.x, curr.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mMode = NONE
            }
        }
        imageMatrix = mMatrix
        return true // indicate event was handled
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mMode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = mSaveScale
            mSaveScale *= mScaleFactor

            if (mSaveScale > mMaxScale) {
                mSaveScale = mMaxScale
                mScaleFactor = mMaxScale / origScale
            } else if (mSaveScale < mMinScale) {
                mSaveScale = mMinScale
                mScaleFactor = mMinScale / origScale
            }

            if (origWidth * mSaveScale <= viewWidth || origHeight * mSaveScale <= viewHeight) {
                mMatrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
            } else {
                mMatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
            }
            fixTranslation()
            return true
        }
    }

    private fun fixTranslation() {
        mMatrix.getValues(mMatrixValues)
        val transX = mMatrixValues[Matrix.MTRANS_X]
        val transY = mMatrixValues[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * mSaveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * mSaveScale)

        if (fixTransX != 0f || fixTransY != 0f) {
            mMatrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = (viewSize - contentSize) / 2f
            maxTrans = minTrans
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        return when {
            trans < minTrans -> minTrans - trans
            trans > maxTrans -> maxTrans - trans
            else -> 0f
        }
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else {
            delta
        }
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val origScale = mSaveScale
        val mScaleFactor: Float

        if (mSaveScale == mMinScale) {
            mSaveScale = mMaxScale
            mScaleFactor = mMaxScale / origScale
        } else {
            mSaveScale = mMinScale
            mScaleFactor = mMinScale / origScale
        }

        mMatrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
        fixTranslation()
        imageMatrix = mMatrix
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false
}
