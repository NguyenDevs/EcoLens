package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemLanguageModernBinding
import com.nguyendevs.ecolens.model.Language

class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolderModern>() {

    inner class LanguageViewHolderModern(private val binding: ItemLanguageModernBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val colorGreenPrimary = ContextCompat.getColor(itemView.context, R.color.green_primary)
        private val colorGreenDark = ContextCompat.getColor(itemView.context, R.color.green_dark)
        private val colorTextPrimary = ContextCompat.getColor(itemView.context, R.color.text_primary)
        // Chuyển đổi 2dp sang pixel để hiển thị đồng đều trên các màn hình
        private val strokeWidthPx = (2 * itemView.resources.displayMetrics.density).toInt()

        init {
            binding.cardLanguage.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLanguageClick(languages[position])
                }
            }
        }

        fun bind(language: Language) {
            binding.ivFlag.setImageResource(language.flagDrawable)
            binding.tvLanguageName.text = language.name

            if (language.isSelected) {
                binding.cardLanguage.strokeWidth = strokeWidthPx
                binding.cardLanguage.strokeColor = colorGreenPrimary
                binding.tvLanguageName.setTextColor(colorGreenDark)
            } else {
                binding.cardLanguage.strokeWidth = 0
                binding.tvLanguageName.setTextColor(colorTextPrimary)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolderModern {
        val binding = ItemLanguageModernBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolderModern(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolderModern, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    fun updateList(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }
}