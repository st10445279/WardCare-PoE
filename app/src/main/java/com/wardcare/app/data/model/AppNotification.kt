package com.wardcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val subtitle: String? = null,
    val type: String = "info",
    val timestamp: Long = System.currentTimeMillis()
)