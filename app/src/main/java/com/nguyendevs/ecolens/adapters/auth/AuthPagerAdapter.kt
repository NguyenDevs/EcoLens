package com.nguyendevs.ecolens.adapters.auth

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nguyendevs.ecolens.R

class AuthPagerAdapter : RecyclerView.Adapter<AuthPagerAdapter.PageViewHolder>() {

        private var loginPage: View? = null
        private var registerPage: View? = null

        class PageViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
                val layoutRes = if (viewType == 0) R.layout.page_login else R.layout.page_register
                val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
                view.layoutParams =
                        ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
                if (position == 0) loginPage = holder.view else registerPage = holder.view
        }

        override fun getItemCount(): Int = 2

        override fun getItemViewType(position: Int): Int = position

        fun getLoginPage(): View? = loginPage
        fun getRegisterPage(): View? = registerPage

        companion object {
                fun setupDynamicHeight(viewPager: ViewPager2) {
                        viewPager.registerOnPageChangeCallback(
                                object : ViewPager2.OnPageChangeCallback() {
                                        override fun onPageSelected(position: Int) {
                                                viewPager.post {
                                                        val recyclerView =
                                                                viewPager.getChildAt(0) as?
                                                                        RecyclerView
                                                                        ?: return@post
                                                        val viewHolder =
                                                                recyclerView
                                                                        .findViewHolderForAdapterPosition(
                                                                                position
                                                                        )
                                                                        ?: return@post
                                                        val child = viewHolder.itemView

                                                        child.measure(
                                                                View.MeasureSpec.makeMeasureSpec(
                                                                        viewPager.width,
                                                                        View.MeasureSpec.EXACTLY
                                                                ),
                                                                View.MeasureSpec.makeMeasureSpec(
                                                                        0,
                                                                        View.MeasureSpec.UNSPECIFIED
                                                                )
                                                        )

                                                        val targetHeight = child.measuredHeight
                                                        if (viewPager.layoutParams.height ==
                                                                        targetHeight
                                                        )
                                                                return@post

                                                        val animator =
                                                                ValueAnimator.ofInt(
                                                                        viewPager.layoutParams
                                                                                .height
                                                                                .coerceAtLeast(0),
                                                                        targetHeight
                                                                )
                                                        animator.duration = 400
                                                        animator.interpolator =
                                                                DecelerateInterpolator()
                                                        animator.addUpdateListener { anim ->
                                                                viewPager.layoutParams =
                                                                        viewPager.layoutParams
                                                                                .apply {
                                                                                        height =
                                                                                                anim.animatedValue as
                                                                                                        Int
                                                                                }
                                                        }
                                                        animator.start()
                                                }
                                        }
                                }
                        )

                        viewPager.post {
                                val recyclerView =
                                        viewPager.getChildAt(0) as? RecyclerView ?: return@post
                                val viewHolder =
                                        recyclerView.findViewHolderForAdapterPosition(0)
                                                ?: return@post
                                val child = viewHolder.itemView

                                child.measure(
                                        View.MeasureSpec.makeMeasureSpec(
                                                viewPager.width,
                                                View.MeasureSpec.EXACTLY
                                        ),
                                        View.MeasureSpec.makeMeasureSpec(
                                                0,
                                                View.MeasureSpec.UNSPECIFIED
                                        )
                                )

                                viewPager.layoutParams =
                                        viewPager.layoutParams.apply {
                                                height = child.measuredHeight
                                        }
                        }
                }
        }
}
