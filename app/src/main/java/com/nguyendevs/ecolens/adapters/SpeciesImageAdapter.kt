package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.databinding.ItemSpeciesImageBinding

class SpeciesImageAdapter :
        ListAdapter<String, SpeciesImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
                ItemSpeciesImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ImageViewHolder(private val binding: ItemSpeciesImageBinding) :
            RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String) {
            Glide.with(binding.root.context)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivSpeciesImage)
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
