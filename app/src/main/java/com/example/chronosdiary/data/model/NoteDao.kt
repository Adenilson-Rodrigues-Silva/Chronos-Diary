package com.example.chronosdiary.data.model // Verifique se o package está igual ao seu Note.kt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {

    // Salva uma nova nota no banco
    @Insert
    suspend fun insert(note: Note)

    // Pega todas as notas da mais nova para a mais antiga
    @Query("SELECT * FROM notes_table ORDER BY id DESC")
    suspend fun getAllNotes(): List<Note>

    // Se quiser deletar tudo depois, já deixamos o comando pronto
    @Query("DELETE FROM notes_table")
    suspend fun deleteAll()
}