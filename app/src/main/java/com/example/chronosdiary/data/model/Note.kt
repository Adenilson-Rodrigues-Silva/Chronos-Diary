package com.example.chronosdiary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var date: String,    // Mantive o campo date que você já tinha
    var content: String
)