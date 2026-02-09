package com.nguyendevs.ecolens.fragments.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.LayoutBottomSheetExportHistoryBinding
import com.nguyendevs.ecolens.utils.ExportUtils.ExportFormat

class ExportHistoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetExportHistoryBinding? = null
    private val binding
        get() = _binding!!

    var onExportConfirmed: ((ExportFormat, Boolean) -> Unit)? = null


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetExportHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expand bottom sheet fully
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        setupListeners()
    }

    private fun setupListeners() {

        binding.radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioJson, R.id.radioXlsx -> {
                    binding.checkIncludeImage.isChecked = false
                    binding.checkIncludeImage.isEnabled = false
                    binding.checkIncludeImage.alpha = 0.5f
                }
                else -> {
                    binding.checkIncludeImage.isEnabled = true
                    binding.checkIncludeImage.alpha = 1.0f
                }
            }
        }

        binding.checkIncludeImage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val currentId = binding.radioGroupFormat.checkedRadioButtonId
                if (currentId == R.id.radioJson || currentId == R.id.radioXlsx) {
                    binding.checkIncludeImage.isChecked = false
                }
            }
        }

        binding.btnConfirmExport.setOnClickListener {
            val format =
                    when (binding.radioGroupFormat.checkedRadioButtonId) {
                        R.id.radioDocx -> ExportFormat.DOCX
                        R.id.radioXlsx -> ExportFormat.XLSX
                        R.id.radioPdf -> ExportFormat.PDF
                        R.id.radioJson -> ExportFormat.JSON
                        else -> ExportFormat.DOCX
                    }

            val includeImage = binding.checkIncludeImage.isChecked

            dismiss()
            onExportConfirmed?.invoke(format, includeImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExportHistoryBottomSheet"
        fun newInstance() = ExportHistoryBottomSheet()
    }
}
