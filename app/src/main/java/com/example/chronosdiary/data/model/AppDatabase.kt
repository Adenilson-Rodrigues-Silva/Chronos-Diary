package com.example.chronosdiary.data // Se você colocar na pasta data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chronosdiary.data.model.Note    // Importa o Note
import com.example.chronosdiary.data.model.NoteDao // Importa o Dao

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronos_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}