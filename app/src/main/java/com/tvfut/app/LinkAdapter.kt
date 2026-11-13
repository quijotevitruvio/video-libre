package com.tvfut.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tvfut.app.databinding.ItemLinkBinding

class LinkAdapter(
    private val items: List<LinkItem>,
    private val onOpen: (LinkItem) -> Unit,
    private val onMenu: (LinkItem, View) -> Unit
) : RecyclerView.Adapter<LinkAdapter.Holder>() {

    inner class Holder(val binding: ItemLinkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.title
        holder.binding.url.text = item.url
        holder.binding.root.setOnClickListener { onOpen(item) }
        holder.binding.root.setOnLongClickListener {
            onMenu(item, holder.binding.more)
            true
        }
        holder.binding.more.setOnClickListener { onMenu(item, it) }
    }

    override fun getItemCount(): Int = items.size
}
