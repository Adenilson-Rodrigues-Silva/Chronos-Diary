package com.example.chronosdiary.ui.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteDetailActivity : AppCompatActivity() {

    // Referências dos componentes do XML
    private lateinit var etContent: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // 1. Inicializar os componentes
        etContent = findViewById(R.id.et_detail_content)
        btnBack = findViewById<ImageButton>(R.id.btn_back)
        tvDate = findViewById(R.id.tv_detail_date)

        // 2. Lógica do botão Voltar
        btnBack.setOnClickListener {
            finish() // Fecha esta tela e volta para o Feed
        }

        // 3. Pegar o ID enviado pelo Adapter e carregar a nota
        // Tente pegar como Int se o Long retornar -1
        // No onCreate da NoteDetailActivity
        val intentId = intent.getLongExtra("NOTE_ID", -1L)
// Se o Long falhar, tentamos buscar como Int (caso o Room esteja usando Int)
        val noteId = if (intentId == -1L) intent.getIntExtra("NOTE_ID", -1).toLong() else intentId

        if (noteId != -1L) {
            loadNoteData(noteId) // Agora garantimos que a função é chamada!
        } else {
            //Toast.makeText(this, "Erro: Nota não encontrada", Toast.LENGTH_SHORT).show()
            Toast.makeText(this@NoteDetailActivity, "Cronologia atualizada!", Toast.LENGTH_SHORT).show()
        }

        // Dentro do onCreate, após inicializar os componentes
        val btnSave = findViewById<ImageButton>(R.id.fab_save_changes) // Use o ID correto do seu XML

        btnSave.setOnClickListener {
            val novoTexto = etContent.text.toString()

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@NoteDetailActivity)
                val noteAtual = db.noteDao().getNoteById(noteId)

                noteAtual?.let {
                    it.content = novoTexto
                    db.noteDao().update(it)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NoteDetailActivity, "Cronologia atualizada!", Toast.LENGTH_SHORT).show()
                        finish() // Volta para o feed
                    }
                }
            }
        }
    }

    private fun loadNoteData(id: Long) {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val note = db.noteDao().getNoteById(id)

            withContext(Dispatchers.Main) {
                if (note != null) {
                    // Use as variáveis que você já inicializou no onCreate
                    etContent.setText(note.content)
                    tvDate.text = note.date
                } else {
                    // Log de erro para você ver no Logcat se a nota veio vazia
                    println("CHRONOS_LOG: Nota com ID $id não encontrada no banco!")
                }
            }
        }
    }
}