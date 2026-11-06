package com.wardcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val patientId: String,
    val patientName: String,
    val medicationName: String,
    val status: String, // given, missed, delayed
    val note: String? = null,
    val scheduledTime: Long,
    val loggedTime: Long = System.currentTimeMillis(),
    val staffId: String,
    val isSyncPending: Boolean = true
)
