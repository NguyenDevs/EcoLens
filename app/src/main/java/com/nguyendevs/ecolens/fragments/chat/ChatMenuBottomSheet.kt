package com.nguyendevs.ecolens.fragments.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetChatMenuBinding

/** Bottom sheet menu cho chat, chứa tùy chọn xóa phiên chat. */
class ChatMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetChatMenuBinding? = null
    private val binding
        get() = _binding!!

    var onDeleteClicked: (() -> Unit)? = null

    /** Inflate layout của bottom sheet. */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetChatMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Thiết lập listener cho nút xóa. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDeleteChat.setOnClickListener {
            dismiss()
            onDeleteClicked?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ChatMenuBottomSheet"

        /** Tạo instance mới của ChatMenuBottomSheet. */
        fun newInstance() = ChatMenuBottomSheet()
    }
}
