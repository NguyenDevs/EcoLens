package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.ChatSession
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class ChatSessionAdapter(
    private var sessions: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.ViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private lateinit var markwon: Markwon

    fun updateList(newList: List<ChatSession>) {
        sessions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!::markwon.isInitialized) {
            markwon = Markwon.create(parent.context)
        }
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_entry_modern, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]

        val isFirstOfDay = position == 0 ||
                dateFormatter.format(Date(session.timestamp)) != dateFormatter.format(Date(sessions[position - 1].timestamp))

        holder.bind(session, isFirstOfDay)
    }

    override fun getItemCount() = sessions.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val card: View = view.findViewById(R.id.cardSession)

        val ivLoadingRing: ImageView = view.findViewById(R.id.ivLoadingRing)

        fun bind(session: ChatSession, showHeader: Boolean) {
            tvDate.visibility = if (showHeader) View.VISIBLE else View.GONE
            tvDate.text = dateFormatter.format(Date(session.timestamp))
            ivLoadingRing.visibility = View.INVISIBLE
            ivLoadingRing.animate().cancel()
            tvTitle.text = session.title
            markwon.setMarkdown(tvLastMessage, session.lastMessage)

            tvTime.text = timeFormatter.format(Date(session.timestamp))

            card.setOnClickListener {
                ivLoadingRing.visibility = View.VISIBLE
                ivLoadingRing.alpha = 1f

                ivLoadingRing.animate()
                    .rotationBy(360f)
                    .setDuration(800)
                    .setInterpolator(android.view.animation.LinearInterpolator())
                    .withEndAction {
                        ivLoadingRing.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                ivLoadingRing.visibility = View.INVISIBLE
                                ivLoadingRing.rotation = 0f
                            }
                            .start()
                    }
                    .start()

                onClick(session)
            }
        }
    }
}