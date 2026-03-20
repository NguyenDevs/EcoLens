package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetHistoryMenuBinding

/** Bottom sheet menu cho chi tiết lịch sử: xóa, tải ảnh, xuất file. */
class HistoryMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetHistoryMenuBinding? = null
    private val binding
        get() = _binding!!

    var onDeleteClicked: (() -> Unit)? = null
    var onDownloadClicked: (() -> Unit)? = null
    var onExportClicked: (() -> Unit)? = null

    /** Inflate layout của bottom sheet. */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetHistoryMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Thiết lập listener cho các nút xóa, tải ảnh và xuất file. */
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

        /** Tạo instance mới của HistoryMenuBottomSheet. */
        fun newInstance() = HistoryMenuBottomSheet()
    }
}
