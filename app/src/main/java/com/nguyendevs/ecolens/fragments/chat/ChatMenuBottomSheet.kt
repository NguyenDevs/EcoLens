package com.nguyendevs.ecolens.fragments.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetChatMenuBinding

class ChatMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetChatMenuBinding? = null
    private val binding
        get() = _binding!!

    var onDeleteClicked: (() -> Unit)? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetChatMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

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
        fun newInstance() = ChatMenuBottomSheet()
    }
}
