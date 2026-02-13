package com.example.chronosdiary.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chronosdiary.ui.activities.NoteDetailActivity
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

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, NoteDetailActivity::class.java)
            // Passamos o ID da nota para a próxima tela saber qual abrir
            intent.putExtra("NOTE_ID", note.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = notes.size
}