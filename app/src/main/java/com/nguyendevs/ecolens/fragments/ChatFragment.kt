package com.nguyendevs.ecolens.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
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
import android.view.inputmethod.InputMethodManager

class ChatFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private val adapter = ChatAdapter()

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView

    private var currentSessionId: Long? = null

    companion object {
        private const val ARG_SESSION_ID = "session_id"

        fun newInstance(sessionId: Long? = null): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    sessionId?.let { putLong(ARG_SESSION_ID, it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSessionId = arguments?.getLong(ARG_SESSION_ID, -1L)?.takeIf { it != -1L }
    }

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

        // FIX: Kiểm tra xem có sessionId không
        if (currentSessionId != null) {
            // Nếu có sessionId -> load chat cũ (KHÔNG tạo mới)
            viewModel.loadChatSession(currentSessionId!!)
        } else {
            // Nếu không có sessionId -> tạo chat mới
            viewModel.initNewChatSession(getString(R.string.chat_welcome))
        }

        etInput.post {
            etInput.requestFocus()
            // val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun initViews(view: View) {
        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBack)
        btnMenu = view.findViewById(R.id.btnMenu)
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
                performHapticFeedback()
                viewModel.sendChatMessage(text)
                etInput.text.clear()
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Menu 3 chấm
        btnMenu.setOnClickListener {
            showMenuPopup(it)
        }
    }

    private fun showMenuPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)

        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_chat -> {
                    showDeleteConfirmDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa đoạn chat")
            .setMessage("Bạn có chắc muốn xóa đoạn chat này không?")
            .setPositiveButton("Xóa") { _, _ ->
                currentSessionId?.let { sessionId ->
                    viewModel.deleteChatSession(sessionId)
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
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

    private fun performHapticFeedback() {
        try {
            val context = requireContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatFragment", "Vibration failed: ${e.message}")
        }
    }
}