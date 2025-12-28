package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.Language

class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardLanguage)
        val ivFlag: ImageView = view.findViewById(R.id.ivFlag)
        val tvLanguageName: TextView = view.findViewById(R.id.tvLanguageName)

        fun bind(language: Language) {
            ivFlag.setImageResource(language.flagDrawable)
            tvLanguageName.text = language.name

            if (language.isSelected) {
                card.strokeWidth = 4
                card.strokeColor = itemView.context.getColor(R.color.green_primary)
                tvLanguageName.setTextColor(itemView.context.getColor(R.color.green_dark))
            } else {
                card.strokeWidth = 0
                tvLanguageName.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            card.setOnClickListener {
                onLanguageClick(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_modern, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    fun updateList(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }
}