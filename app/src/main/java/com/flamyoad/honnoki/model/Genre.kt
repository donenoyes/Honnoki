package com.flamyoad.honnoki.model

import androidx.room.Entity

@Entity(tableName = "genres")
data class Genre(
    val id: Long? = null,
    val name: String,
    val link: String
)