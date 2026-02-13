package com.example.chronosdiary.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.R

// Mudamos para 'var' e 'MutableList' para podermos alterar a lista depois
class NoteAdapter(private var notes: List<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.text_note_date)
        val previewText: TextView = view.findViewById(R.id.text_note_preview)
    }

    // --- ESTA É A FUNÇÃO QUE ESTAVA FALTANDO ---
    fun updateNotes(newNotes: List<Note>) {
        this.notes = newNotes
        notifyDataSetChanged() // Isso força a lista na tela a se atualizar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.dateText.text = note.date
        holder.previewText.text = note.content
    }

    override fun getItemCount() = notes.size
}