package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemLanguagesBinding
import com.nguyendevs.ecolens.models.Language

/** Adapter hiển thị danh sách ngôn ngữ có thể chọn. */
class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolderModern>() {

    /** Tạo ViewHolder cho item ngôn ngữ. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolderModern {
        val binding = ItemLanguagesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolderModern(binding)
    }

    /** Bind dữ liệu vào ViewHolder. */
    override fun onBindViewHolder(holder: LanguageViewHolderModern, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    /** Cập nhật danh sách ngôn ngữ mới. */
    fun updateList(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }

    /** ViewHolder hiển thị một item ngôn ngữ với icon cờ và tên. */
    inner class LanguageViewHolderModern(
        private val binding: ItemLanguagesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val colorPrimary = ContextCompat.getColor(itemView.context, R.color.primary)
        private val colorTextPrimary = ContextCompat.getColor(itemView.context, R.color.text_primary)
        private val colorSurface = ContextCompat.getColor(itemView.context, R.color.surface)
        private val strokeWidthPx = (2 * itemView.resources.displayMetrics.density).toInt()

        init {
            binding.cardLanguage.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLanguageClick(languages[position])
                }
            }
        }

        /** Hiển thị thông tin ngôn ngữ và đánh dấu nếu đang được chọn. */
        fun bind(language: Language) {
            binding.ivFlag.setImageResource(language.flagDrawable)
            binding.tvLanguageName.text = language.name
            binding.cardLanguage.setCardBackgroundColor(colorSurface)

            if (language.isSelected) {
                binding.cardLanguage.strokeWidth = strokeWidthPx
                binding.cardLanguage.strokeColor = colorPrimary
                binding.tvLanguageName.setTextColor(colorPrimary)
            } else {
                binding.cardLanguage.strokeWidth = 0
                binding.tvLanguageName.setTextColor(colorTextPrimary)
            }
        }
    }
}