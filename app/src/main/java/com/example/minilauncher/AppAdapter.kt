package com.example.minilauncher

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val intent: Intent
)

class AppAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private val items = mutableListOf<AppInfo>()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener {
            onLongClick(app)
            true
        }

        // Фокус: карточка чуть увеличивается, подпись становится ярче
        applyFocusState(holder, holder.itemView.isFocused, animate = false)
        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            applyFocusState(holder, hasFocus, animate = true)
        }
    }

    private fun applyFocusState(holder: VH, focused: Boolean, animate: Boolean) {
        val scale = if (focused) 1.15f else 1f
        val alpha = if (focused) 1f else 0.6f
        if (animate) {
            holder.itemView.animate().scaleX(scale).scaleY(scale).setDuration(180).start()
            holder.label.animate().alpha(alpha).setDuration(180).start()
        } else {
            holder.itemView.scaleX = scale
            holder.itemView.scaleY = scale
            holder.label.alpha = alpha
        }
    }
}
