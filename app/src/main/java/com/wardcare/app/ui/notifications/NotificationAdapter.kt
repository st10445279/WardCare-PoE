package com.wardcare.app.ui.notifications

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wardcare.app.data.model.AppNotification
import com.wardcare.app.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter :
    ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.tvNotificationTitle.text = notification.title

            val sub = notification.subtitle
            if (!sub.isNullOrBlank()) {
                binding.tvNotificationSubtitle.visibility = View.VISIBLE
                binding.tvNotificationSubtitle.text = sub as CharSequence
            } else {
                binding.tvNotificationSubtitle.visibility = View.GONE
            }

            val formattedTime = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())
                .format(Date(notification.timestamp))
            binding.tvNotificationTime.text = formattedTime

            val (tintColor, strokeColor) = when (notification.type.lowercase(Locale.ROOT)) {
                "given" -> Pair(Color.parseColor("#2E7D32"), Color.parseColor("#A5D6A7"))
                "missed", "overdue" -> Pair(Color.parseColor("#C62828"), Color.parseColor("#EF9A9A"))
                "delayed" -> Pair(Color.parseColor("#EF6C00"), Color.parseColor("#FFCC80"))
                "appointment" -> Pair(Color.parseColor("#1565C0"), Color.parseColor("#90CAF9"))
                else -> Pair(Color.parseColor("#2150A2"), Color.parseColor("#2150A2"))
            }

            binding.ivNotificationIcon.setColorFilter(tintColor)
            binding.root.strokeColor = strokeColor
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem == newItem
        }
    }
}