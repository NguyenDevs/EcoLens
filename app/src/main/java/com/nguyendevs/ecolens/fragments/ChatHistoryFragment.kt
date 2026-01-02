package com.nguyendevs.ecolens.fragments

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatSessionAdapter
import com.nguyendevs.ecolens.databinding.ScreenAssistantModernBinding
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatHistoryFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: ScreenAssistantModernBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatSessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenAssistantModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeChatSessions()
        setupFabListener()
    }

    private fun setupRecyclerView() {
        adapter = ChatSessionAdapter(emptyList()) { session ->
            openChatScreen(session.id)
        }

        binding.rvChatHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatHistoryFragment.adapter
        }
    }

    private fun observeChatSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allChatSessions.collectLatest { list ->
                if (list.isEmpty()) {
                    binding.rvChatHistory.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.VISIBLE
                } else {
                    binding.rvChatHistory.visibility = View.VISIBLE
                    binding.emptyStateContainer.visibility = View.GONE
                }
                adapter.updateList(list)
            }
        }
    }

    private fun setupFabListener() {
        binding.fabNewChat.setOnClickListener {
            performHapticFeedback()
            openChatScreen(null)
        }
    }

    private fun openChatScreen(sessionId: Long?) {
        val fragment = ChatFragment.newInstance(sessionId)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("chat_detail")
            .commit()
    }

    private fun performHapticFeedback() {
        binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}