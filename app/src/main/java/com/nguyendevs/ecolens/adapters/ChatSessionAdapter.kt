package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.ChatSession
import java.text.SimpleDateFormat
import java.util.*

class ChatSessionAdapter(
    private var sessions: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.ViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun updateList(newList: List<ChatSession>) {
        sessions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        // Logic ẩn hiện ngày tháng
        val isFirstOfDay = position == 0 ||
                dateFormatter.format(Date(session.timestamp)) != dateFormatter.format(Date(sessions[position-1].timestamp))

        holder.bind(session, isFirstOfDay)
    }

    override fun getItemCount() = sessions.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val card: View = view.findViewById(R.id.cardSession)

        fun bind(session: ChatSession, showHeader: Boolean) {
            tvDate.visibility = if (showHeader) View.VISIBLE else View.GONE
            tvDate.text = dateFormatter.format(Date(session.timestamp))

            tvTitle.text = session.title
            tvLastMessage.text = session.lastMessage
            tvTime.text = timeFormatter.format(Date(session.timestamp))

            card.setOnClickListener { onClick(session) }
        }
    }
}