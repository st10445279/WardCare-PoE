package com.wardcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val name: String,
    val bedNumber: String,
    val ward: String = "General Ward",
    val age: Int = 45,
    val gender: String = "Male",
    val medicalHistory: String = "Hypertension, Type 2 Diabetes",
    val allergies: String = "Penicillin",
    val bloodType: String = "O+",
    val nextAppointment: String = "14:00",
    val medSchedule: String = "Amoxicillin 8h (08:00 AM, 16:00 PM, 20:00 PM)"
)
