package com.unsa.examendanp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val encounteredUserId: String,
    val timestamp: Date,
    val duration: Long,
    val rssi: Int,
    val distance: Float,
    val isSynced: Boolean = false
)