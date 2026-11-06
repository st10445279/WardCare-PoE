package com.wardcare.app.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.databinding.ItemMedicationLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationLogAdapter :
    ListAdapter<MedicationLog, MedicationLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemMedicationLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(log: MedicationLog) {
            binding.tvPatientName.text = log.patientName
            binding.tvPatientId.text = "Patient ID: ${log.patientId}"
            binding.tvMedicationName.text = log.medicationName
            binding.tvLoggedTime.text = "Logged: ${dateFormat.format(Date(log.loggedTime))}"

            if (!log.note.isNull_or_empty_or_blank()) {
                binding.tvNote.text = log.note
                binding.tvNote.visibility = View.VISIBLE
            } else {
                binding.tvNote.visibility = View.GONE
            }

            // Status chip styling
            binding.tvStatus.text = log.status.uppercase(Locale.getDefault())
            when (log.status.lowercase(Locale.getDefault())) {
                "given" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                "missed" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#C62828"))
                }
                "delayed" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#EF6C00"))
                }
                else -> {
                    binding.tvStatus.setTextColor(Color.GRAY)
                }
            }

            // Sync status styling
            if (log.isSyncPending) {
                binding.tvSyncStatus.text = "Pending Sync"
                binding.tvSyncStatus.setTextColor(Color.parseColor("#EF6C00"))
            } else {
                binding.tvSyncStatus.text = "Synced"
                binding.tvSyncStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
        }

        private fun String?.isNull_or_empty_or_blank(): Boolean {
            return this.isNullOrBlank()
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MedicationLog>() {
        override fun areItemsTheSame(oldItem: MedicationLog, newItem: MedicationLog): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(oldItem: MedicationLog, newItem: MedicationLog): Boolean {
            return oldItem == newItem
        }
    }
}
