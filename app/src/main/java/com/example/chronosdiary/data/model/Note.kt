package com.example.chronosdiary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,    // Mantive o campo date que você já tinha
    val content: String
)