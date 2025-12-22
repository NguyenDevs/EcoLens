package com.nguyendevs.ecolens.fragments

import android.content.*
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment(), ChatAdapter.OnChatActionListener {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private lateinit var adapter: ChatAdapter

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
                arguments = Bundle().apply { sessionId?.let { putLong(ARG_SESSION_ID, it) } }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSessionId = arguments?.getLong(ARG_SESSION_ID, -1L)?.takeIf { it != -1L }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatAdapter(this)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (currentSessionId != null) {
            viewModel.loadChatSession(currentSessionId!!)
        } else {
            viewModel.initNewChatSession(getString(R.string.chat_welcome), getString(R.string.new_chat))
        }

        etInput.post { etInput.requestFocus() }
    }

    private fun initViews(view: View) {
        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSend)
        btnBack = view.findViewById(R.id.btnBack)
        btnMenu = view.findViewById(R.id.btnMenu)
    }

    private fun setupRecyclerView() {
        rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvChat.adapter = adapter
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                performHapticFeedback()
                viewModel.sendChatMessage(text, getString(R.string.new_chat))
                etInput.text.clear()
            }
        }
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnMenu.setOnClickListener { showMenuPopup(it) }
    }

    // --- Triển khai OnChatActionListener ---
    override fun onCopy(text: String) {
        performHapticFeedback()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("EcoLens", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
    }

    override fun onShare(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
    }

    override fun onRenew(position: Int, message: ChatMessage) {
        val messages = viewModel.chatMessages.value
        if (position > 0) {
            val userMsg = messages[position - 1]
            if (userMsg.isUser) {
                performHapticFeedback()
                viewModel.renewAiResponse(message, userMsg.content)
            }
        }
    }

    // --- Giữ nguyên các hàm phụ trợ của bạn ---
    private fun showMenuPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType).invoke(mPopup, true)
        } catch (e: Exception) { e.printStackTrace() }

        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_delete_chat) { showDeleteConfirmDialog(); true } else false
        }
        popup.show()
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_chat_title).setMessage(R.string.dialog_delete_chat_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                currentSessionId?.let { viewModel.deleteChatSession(it); parentFragmentManager.popBackStack() }
            }
            .setNegativeButton(R.string.action_cancel, null).show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                adapter.submitList(messages)
                if (messages.isNotEmpty()) rvChat.post { rvChat.smoothScrollToPosition(messages.size - 1) }
            }
        }
    }

    private fun performHapticFeedback() {
        try {
            val context = requireContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) { Log.e("ChatFragment", "Vibration failed: ${e.message}") }
    }
}