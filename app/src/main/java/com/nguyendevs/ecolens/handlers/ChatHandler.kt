package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatHandler(
    private val context: Context,
    private val view: View,
    private val viewModel: EcoLensViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onExitChat: () -> Unit
) {
    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private val adapter = ChatAdapter()

    fun setup() {
        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBackChat)

        rvChat.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter

        if (viewModel.chatMessages.value.isEmpty()) {
            viewModel.sendChatMessage(context.getString(R.string.chat_welcome))
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendChatMessage(text)
                etInput.text.clear()
            }
        }

        btnBack.setOnClickListener {
            onExitChat()
        }

        lifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }
}