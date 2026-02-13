package com.example.chronosdiary.data.model // Verifique se o package está igual ao seu Note.kt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {

    @Update
    suspend fun update(note: Note)

    // Salva uma nova nota no banco
    @Insert
    suspend fun insert(note: Note)

    // Pega todas as notas da mais nova para a mais antiga
    @Query("SELECT * FROM notes_table ORDER BY id DESC")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes_table WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    // Se quiser deletar tudo depois, já deixamos o comando pronto
    @Query("DELETE FROM notes_table")
    suspend fun deleteAll()


}