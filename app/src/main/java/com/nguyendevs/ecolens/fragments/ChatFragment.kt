package com.nguyendevs.ecolens.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private val adapter = ChatAdapter()

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (viewModel.chatMessages.value.isEmpty()) {
            viewModel.sendChatMessage(getString(R.string.chat_welcome))
        }
    }

    private fun initViews(view: View) {
        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendChatMessage(text)
                etInput.text.clear()
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    rvChat.post {
                        rvChat.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }
}