package com.unsa.examendanp.data.remote.dto

import com.google.firebase.firestore.PropertyName

data class ContactDto(
    @PropertyName("id")
    val id: String = "",

    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("encounteredUserId")
    val encounteredUserId: String = "",

    @PropertyName("timestamp")
    val timestamp: Long = 0L,

    @PropertyName("duration")
    val duration: Long = 0L,

    @PropertyName("rssi")
    val rssi: Int = 0,

    @PropertyName("distance")
    val distance: Float = 0f
)