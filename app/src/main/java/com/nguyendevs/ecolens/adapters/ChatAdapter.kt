package com.nguyendevs.ecolens.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemChatMessageBinding
import com.nguyendevs.ecolens.models.chat.ChatMessage
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val actionListener: OnChatActionListener) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnChatActionListener {
        fun onCopy(text: String)
        fun onShare(text: String)
        fun onRenew(position: Int, message: ChatMessage)
    }

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var markwon: Markwon
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var expandedPosition = -1

    fun collapseExpandedActions() {
        if (expandedPosition != -1) {
            val prev = expandedPosition
            expandedPosition = -1
            notifyItemChanged(prev, "TOGGLE_ACTIONS")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        if (!::markwon.isInitialized) {
            markwon =
                    Markwon.builder(parent.context)
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(
                                    object : AbstractMarkwonPlugin() {
                                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                                            builder.headingTextSizeMultipliers(
                                                    floatArrayOf(
                                                            2.0f,
                                                            1.5f,
                                                            1.17f,
                                                            1.0f,
                                                            0.83f,
                                                            0.67f
                                                    )
                                            )
                                        }
                                    }
                            )
                            .build()
        }
        val binding =
                ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(
            holder: ChatViewHolder,
            position: Int,
            payloads: MutableList<Any>
    ) {
        if (payloads.contains("TOGGLE_ACTIONS")) {
            val isExpanded = position == expandedPosition
            holder.binding.layoutActions.setExpanded(isExpanded, true)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position], position)
    }

    override fun onViewRecycled(holder: ChatViewHolder) {
        super.onViewRecycled(holder)
        holder.stopAnimation()
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<ChatMessage>) {
        val oldSize = messages.size
        val newSize = newMessages.size

        messages.clear()
        messages.addAll(newMessages)

        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
            if (oldSize > 0) notifyItemChanged(oldSize - 1)
        } else if (newSize == oldSize && newSize > 0) {
            val lastMsg = messages[newSize - 1]
            if (lastMsg.isStreaming) {
                notifyItemChanged(newSize - 1, "STREAMING")
            } else {
                notifyItemChanged(newSize - 1)
            }
        } else {
            notifyDataSetChanged()
        }
    }

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {

        private val colorWhite = ContextCompat.getColor(itemView.context, R.color.white)
        private val colorPrimary = ContextCompat.getColor(itemView.context, R.color.primary)
        private val colorPrimaryLight =
                ContextCompat.getColor(itemView.context, R.color.primary_light)
        private val colorEmerald50 = ContextCompat.getColor(itemView.context, R.color.emerald_50)
        private val colorEmerald100 = ContextCompat.getColor(itemView.context, R.color.emerald_100)
        private val colorBorderLight =
                ContextCompat.getColor(itemView.context, R.color.border_light)
        private val colorPrimaryDark =
                ContextCompat.getColor(itemView.context, R.color.primary_dark)

        private val radiusMd = itemView.resources.getDimension(R.dimen.radius_md)
        private val radiusSmall = itemView.resources.getDimension(R.dimen.radius_xxs)

        private val handler = Handler(Looper.getMainLooper())
        private var loopCount = 0
        private var typingAnimatorSet: android.animation.AnimatorSet? = null

        private fun getThemeColor(attr: Int): Int {
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        private val aiBubbleShape =
                ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radiusSmall)
                        .setTopRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .build()

        private val userBubbleShape =
                ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .setTopRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomRightCorner(CornerFamily.ROUNDED, radiusSmall)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .build()

        private val loadingAnimateRunnable =
                object : Runnable {
                    override fun run() {
                        loopCount++
                        val spannable = SpannableString("...")
                        val visibleDots = (loopCount % 3) + 1
                        if (visibleDots < 3) {
                            spannable.setSpan(
                                    ForegroundColorSpan(Color.TRANSPARENT),
                                    visibleDots,
                                    3,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        binding.tvMessage.text = spannable
                        handler.postDelayed(this, 400)
                    }
                }

        fun stopAnimation() {
            handler.removeCallbacks(loadingAnimateRunnable)
            stopTypingAnimation()
            binding.tvMessage.alpha = 1f
        }

        fun bind(message: ChatMessage, position: Int) {
            stopAnimation()
            resetViews()
            val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
            val colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface)

            showTimestamp(message)

            when {
                message.isLoading -> bindLoadingState(colorSurface, colorOnSurface)
                message.isStreaming -> bindStreamingState(colorSurface, colorOnSurface)
                message.isUser -> bindUserMessage(message, position)
                else -> bindAiMessage(message, position, colorSurface, colorOnSurface)
            }
        }

        private fun resetViews() {
            binding.tvMessage.visibility = View.VISIBLE
            binding.layoutTypingIndicator.visibility = View.GONE
            binding.tvMessage.alpha = 1f
            binding.tvMessage.setTypeface(null, Typeface.NORMAL)
            binding.tvTimestamp.visibility = View.GONE
            binding.layoutMessageContent.background = null
            binding.expandableText.setExpanded(true, false)
            binding.cardMessage.strokeWidth = 0
            binding.cardMessage.cardElevation =
                    itemView.resources.getDimension(R.dimen.elevation_xs)
            binding.cardMessage.setOnClickListener(null)
            binding.cardMessage.setOnLongClickListener(null)
            binding.tvMessage.setOnLongClickListener(null)
            binding.layoutActions.setExpanded(false, false)
        }

        private fun showTimestamp(message: ChatMessage) {
            binding.tvTimestamp.visibility = View.VISIBLE
            binding.tvTimestamp.text = timeFormat.format(Date(message.timestamp))
        }

        private fun configureAiLayout() {
            binding.cardMiniAvatar.visibility = View.VISIBLE
            binding.spacerEnd.visibility = View.VISIBLE
            binding.spacerAvatarFooter.visibility = View.VISIBLE
            binding.chatContainer.gravity = Gravity.START
            binding.layoutBubbleRow.gravity = Gravity.START
            binding.layoutFooterRow.gravity = Gravity.START

            binding.cardMessage.shapeAppearanceModel = aiBubbleShape
            binding.viewAccentBar.visibility = View.GONE
            binding.cardMessage.strokeWidth = 1
            binding.cardMessage.strokeColor = colorBorderLight

            val leftBorderWidthPx = 3f * itemView.resources.displayMetrics.density
            val leftBorderDrawable =
                    object : android.graphics.drawable.Drawable() {
                        val shapeDrawable =
                                com.google.android.material.shape.MaterialShapeDrawable(
                                                aiBubbleShape
                                        )
                                        .apply {
                                            fillColor =
                                                    android.content.res.ColorStateList.valueOf(
                                                            colorPrimaryLight
                                                    )
                                        }

                        override fun draw(canvas: android.graphics.Canvas) {
                            shapeDrawable.bounds = bounds
                            canvas.save()
                            canvas.clipRect(0f, 0f, leftBorderWidthPx, bounds.height().toFloat())
                            shapeDrawable.draw(canvas)
                            canvas.restore()
                        }

                        override fun setAlpha(alpha: Int) {}
                        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                        @Suppress("DEPRECATION")
                        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                    }
            binding.layoutMessageContent.background = leftBorderDrawable
        }

        private fun configureUserLayout() {
            binding.cardMiniAvatar.visibility = View.GONE
            binding.spacerEnd.visibility = View.GONE
            binding.spacerAvatarFooter.visibility = View.GONE
            binding.chatContainer.gravity = Gravity.END
            binding.layoutBubbleRow.gravity = Gravity.END

            val params =
                    binding.tvTimestamp.layoutParams as android.widget.LinearLayout.LayoutParams
            params.gravity = Gravity.END
            binding.tvTimestamp.layoutParams = params
            binding.layoutFooterRow.gravity = Gravity.END

            binding.cardMessage.shapeAppearanceModel = userBubbleShape
            binding.viewAccentBar.visibility = View.GONE
            binding.layoutMessageContent.background = null
            binding.cardMessage.strokeWidth = 0
        }

        private fun bindLoadingState(bgColor: Int, textColor: Int) {
            configureAiLayout()
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            binding.tvMessage.text = "..."
            binding.tvTimestamp.visibility = View.GONE
            loopCount = 0
            loadingAnimateRunnable.run()
        }

        private fun bindStreamingState(bgColor: Int, textColor: Int) {
            configureAiLayout()
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            binding.tvMessage.text = ""
            binding.expandableText.setExpanded(false, false)
            startTypingAnimation()
        }

        private fun bindUserMessage(message: ChatMessage, position: Int) {
            configureUserLayout()
            markwon.setMarkdown(binding.tvMessage, message.content)
            binding.cardMessage.setCardBackgroundColor(colorPrimary)
            binding.tvMessage.setTextColor(colorWhite)
            binding.cardMessage.cardElevation =
                    itemView.resources.getDimension(R.dimen.elevation_sm)
            binding.cardMessage.setOnLongClickListener {
                actionListener.onCopy(message.content)
                true
            }
            binding.tvMessage.setOnLongClickListener {
                actionListener.onCopy(message.content)
                true
            }

            val collapseListener =
                    View.OnClickListener {
                        if (expandedPosition != -1) {
                            val prev = expandedPosition
                            expandedPosition = -1
                            (binding.root.parent as? RecyclerView)?.adapter?.notifyItemChanged(
                                    prev,
                                    "TOGGLE_ACTIONS"
                            )
                        }
                    }
            binding.root.setOnClickListener(collapseListener)
            binding.cardMessage.setOnClickListener(collapseListener)
            binding.tvMessage.setOnClickListener(collapseListener)
        }

        private fun bindAiMessage(
                message: ChatMessage,
                position: Int,
                bgColor: Int,
                textColor: Int
        ) {
            configureAiLayout()

            if (position == 0) {
                binding.cardMessage.setCardBackgroundColor(colorEmerald50)
                binding.cardMessage.strokeColor = colorEmerald100
                binding.tvMessage.setTextColor(colorPrimaryDark)
                binding.tvMessage.setTypeface(null, Typeface.ITALIC)
            } else {
                binding.cardMessage.setCardBackgroundColor(bgColor)
                binding.tvMessage.setTextColor(textColor)
            }

            markwon.setMarkdown(binding.tvMessage, message.content)
            binding.expandableText.setExpanded(true, true)

            if (position > 0) {
                val isExpanded = position == expandedPosition
                binding.layoutActions.setExpanded(isExpanded, false)

                val longClickListener =
                        View.OnLongClickListener {
                            val prevExpanded = expandedPosition
                            expandedPosition = if (isExpanded) -1 else position

                            if (prevExpanded != -1) {
                                (binding.root.parent as? RecyclerView)?.adapter?.notifyItemChanged(
                                        prevExpanded,
                                        "TOGGLE_ACTIONS"
                                )
                            }
                            if (expandedPosition != -1) {
                                (binding.root.parent as? RecyclerView)?.adapter?.notifyItemChanged(
                                        expandedPosition,
                                        "TOGGLE_ACTIONS"
                                )
                            }
                            true
                        }

                binding.cardMessage.setOnLongClickListener(longClickListener)
                binding.tvMessage.setOnLongClickListener(longClickListener)

                val collapseListener =
                        View.OnClickListener {
                            if (expandedPosition != -1 && expandedPosition != position) {
                                val prev = expandedPosition
                                expandedPosition = -1
                                (binding.root.parent as? RecyclerView)?.adapter?.notifyItemChanged(
                                        prev,
                                        "TOGGLE_ACTIONS"
                                )
                            } else if (expandedPosition == position) {
                                expandedPosition = -1
                                (binding.root.parent as? RecyclerView)?.adapter?.notifyItemChanged(
                                        position,
                                        "TOGGLE_ACTIONS"
                                )
                            }
                        }
                binding.root.setOnClickListener(collapseListener)
                binding.cardMessage.setOnClickListener(collapseListener)
                binding.tvMessage.setOnClickListener(collapseListener)

                binding.btnCopy.setOnClickListener {
                    actionListener.onCopy(message.content)
                    expandedPosition = -1
                    binding.layoutActions.collapse()
                }

                binding.btnShare.setOnClickListener {
                    actionListener.onShare(message.content)
                    expandedPosition = -1
                    binding.layoutActions.collapse()
                }

                if (position == messages.size - 1) {
                    binding.btnRegenerate.visibility = View.VISIBLE
                    binding.btnRegenerate.setOnClickListener {
                        actionListener.onRenew(position, message)
                        expandedPosition = -1
                        binding.layoutActions.collapse()
                    }
                } else {
                    binding.btnRegenerate.visibility = View.GONE
                }
            } else {
                binding.layoutActions.setExpanded(false, false)
            }
        }

        private fun startTypingAnimation() {
            binding.layoutTypingIndicator.visibility = View.VISIBLE
            binding.tvMessage.visibility = View.GONE

            if (typingAnimatorSet?.isRunning == true) return

            val dot1 = binding.dot1
            val dot2 = binding.dot2
            val dot3 = binding.dot3

            val bounceAnim1 = createBounceAnimator(dot1, 0)
            val bounceAnim2 = createBounceAnimator(dot2, 200)
            val bounceAnim3 = createBounceAnimator(dot3, 400)

            typingAnimatorSet =
                    android.animation.AnimatorSet().apply {
                        playTogether(bounceAnim1, bounceAnim2, bounceAnim3)
                        start()
                    }
        }

        private fun stopTypingAnimation() {
            binding.layoutTypingIndicator.visibility = View.GONE
            binding.tvMessage.visibility = View.VISIBLE
            typingAnimatorSet?.cancel()
            typingAnimatorSet = null
            binding.dot1.translationY = 0f
            binding.dot2.translationY = 0f
            binding.dot3.translationY = 0f
            binding.dot1.alpha = 1f
            binding.dot2.alpha = 1f
            binding.dot3.alpha = 1f
        }

        private fun createBounceAnimator(target: View, startDelayMs: Long): ValueAnimator {
            val distancePx = -6f * target.context.resources.displayMetrics.density
            return ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                startDelay = startDelayMs
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    var ty = 0f
                    var alpha = 0.5f

                    if (fraction <= 0.4f) {
                        val progress = fraction / 0.4f
                        ty = distancePx * progress
                        alpha = 0.5f + (0.5f * progress)
                    } else if (fraction <= 0.8f) {
                        val progress = (fraction - 0.4f) / 0.4f
                        ty = distancePx * (1f - progress)
                        alpha = 1f - (0.5f * progress)
                    }

                    target.translationY = ty
                    target.alpha = alpha
                }
            }
        }
    }
}
