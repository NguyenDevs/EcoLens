package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetHistoryMenuBinding

class HistoryMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetHistoryMenuBinding? = null
    private val binding
        get() = _binding!!

    var onDeleteClicked: (() -> Unit)? = null
    var onDownloadClicked: (() -> Unit)? = null
    var onExportClicked: (() -> Unit)? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetHistoryMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDelete.setOnClickListener {
            dismiss()
            onDeleteClicked?.invoke()
        }

        binding.btnDownload.setOnClickListener {
            dismiss()
            onDownloadClicked?.invoke()
        }

        binding.btnExport.setOnClickListener {
            dismiss()
            onExportClicked?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HistoryMenuBottomSheet"
        fun newInstance() = HistoryMenuBottomSheet()
    }
}
