package com.unsa.examendanp.domain.model

import java.util.Date

data class Contact(
    val id: String,
    val userId: String,
    val encounteredUserId: String,
    val timestamp: Date,
    val duration: Long,
    val rssi: Int,
    val distance: Float,
    val isSynced: Boolean = false
)