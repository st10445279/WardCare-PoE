package com.wardcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val ward: String,
    val passwordHash: String,
    val role: String = "Registered Nurse",
    val staffId: String = "NURSE-9042",
    val resetToken: String? = null,
    val phoneNumber: String = ""
)
