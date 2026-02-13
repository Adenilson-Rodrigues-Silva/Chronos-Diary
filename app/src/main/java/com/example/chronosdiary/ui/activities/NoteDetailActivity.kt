package com.example.chronosdiary.ui.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteDetailActivity : AppCompatActivity() {

    // Referências Globais
    private lateinit var etContent: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDate: TextView
    private lateinit var imgSelectedMood: ImageView
    private lateinit var barFormatting: CardView
    private lateinit var barMoodSelection: CardView
    private var noteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // 1. INICIALIZAÇÃO DOS COMPONENTES
        etContent = findViewById(R.id.et_detail_content)
        btnBack = findViewById(R.id.btn_back)
        tvDate = findViewById(R.id.tv_detail_date)
        imgSelectedMood = findViewById(R.id.img_selected_mood)
        barFormatting = findViewById(R.id.bar_formatting)
        barMoodSelection = findViewById(R.id.bar_mood_selection)

        val btnToggleFormat = findViewById<ImageButton>(R.id.btn_toggle_format)
        val btnToggleMood = findViewById<ImageButton>(R.id.btn_toggle_mood)
        val btnSave = findViewById<ImageButton>(R.id.fab_save_changes)
        val btnMarker = findViewById<ImageButton>(R.id.btn_text_color)

        // 2. RECUPERAR ID DA NOTA
        val intentId = intent.getLongExtra("NOTE_ID", -1L)
        noteId = if (intentId == -1L) intent.getIntExtra("NOTE_ID", -1).toLong() else intentId

        if (noteId != -1L) {
            loadNoteData(noteId)
        }

        // 3. LÓGICA DE ABRIR/FECHAR MENUS (TOGGLE)
        btnToggleFormat.setOnClickListener {
            barFormatting.visibility = if (barFormatting.visibility == View.GONE) View.VISIBLE else View.GONE
            barMoodSelection.visibility = View.GONE
        }

        btnToggleMood.setOnClickListener {
            barMoodSelection.visibility = if (barMoodSelection.visibility == View.GONE) View.VISIBLE else View.GONE
            barFormatting.visibility = View.GONE
        }

        btnBack.setOnClickListener { finish() }

        // 4. LÓGICA DE CADA EMOJI (TODOS OS 7)
        configurarCliquesDosEmojis()

        // 5. LÓGICA DE FORMATAÇÃO NEGRITO E ITALICO
        findViewById<ImageButton>(R.id.format_bold).setOnClickListener { aplicarEstiloTexto(Typeface.BOLD) }
        findViewById<ImageButton>(R.id.format_italic).setOnClickListener { aplicarEstiloTexto(Typeface.ITALIC) }

        // 3. Botão SUBLINHADO (Underline)
        findViewById<ImageButton>(R.id.format_underline).setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd

            if (start != end) {
                val spannable = etContent.text
                // Busca se já existe um sublinhado na seleção
                val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)

                if (spans.isNotEmpty()) {
                    // Se já existir, remove todos (desliga o sublinhado)
                    for (span in spans) spannable.removeSpan(span)
                } else {
                    // Se não existir, aplica (liga o sublinhado)
                    spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                Toast.makeText(this, "Selecione o texto primeiro", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Botão MARCADOR (Highlight) - Use o ID do seu botão de paleta
        btnMarker.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd

            if (start != end) {
                val spannable = etContent.text
                // Busca se já existe uma cor de fundo na seleção
                val spans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)

                if (spans.isNotEmpty()) {
                    // Se já estiver pintado, limpa a cor
                    for (span in spans) spannable.removeSpan(span)
                } else {
                    // Se não estiver, pinta com Ciano Neon transparente
                    val corNeon = Color.parseColor("#4D00FFCC")
                    spannable.setSpan(BackgroundColorSpan(corNeon), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                Toast.makeText(this, "Selecione o texto primeiro", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. BOTÃO SALVAR
        btnSave.setOnClickListener {
            salvarNota()
        }
    }

    private fun configurarCliquesDosEmojis() {
        // Mapeia cada botão para seu ícone e cor correspondente
        val moods = mapOf(
            R.id.select_mood_very_sad to Pair(R.drawable.ic_mod_very_sad, "#FF4444"),
            R.id.select_mood_sad to Pair(R.drawable.ic_mod_sad, "#FF8800"),
            R.id.select_mood_stress to Pair(R.drawable.ic_mod_stress, "#FF00FF"),
            R.id.select_mood_neutral to Pair(R.drawable.ic_mod_neutral, "#00FFCC"),
            R.id.select_mood_happy to Pair(R.drawable.ic_mod_happy, "#AAFF00"),
            R.id.select_mood_very_happy to Pair(R.drawable.ic_mod_very_happy, "#00FF00"),
            R.id.select_mood_very_angry to Pair(R.drawable.ic_mod_very_angry, "#FF0000")
        )

        moods.forEach { (id, data) ->
            findViewById<ImageButton>(id).setOnClickListener {
                imgSelectedMood.setImageResource(data.first)
                imgSelectedMood.imageTintList = ColorStateList.valueOf(Color.parseColor(data.second))
                barMoodSelection.visibility = View.GONE
                Toast.makeText(this, "Humor atualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNoteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val note = db.noteDao().getNoteById(id)
            withContext(Dispatchers.Main) {
                note?.let {
                    etContent.setText(it.content)
                    tvDate.text = it.date
                }
            }
        }
    }

    private fun aplicarEstiloTexto(estilo: Int) {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd

        if (start != end) {
            val spannable = etContent.text

            // 1. Busca todos os estilos (StyleSpan) no pedaço selecionado
            val estilosExistentes = spannable.getSpans(start, end, StyleSpan::class.java)
            var jaTemEstilo = false

            // 2. Verifica se o estilo que clicamos (Negrito ou Itálico) já está lá
            for (span in estilosExistentes) {
                if (span.style == estilo) {
                    spannable.removeSpan(span) // Se já existe, nós removemos!
                    jaTemEstilo = true
                }
            }

            // 3. Se não tinha o estilo, aí sim nós aplicamos
            if (!jaTemEstilo) {
                spannable.setSpan(
                    StyleSpan(estilo),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            Toast.makeText(this, "Selecione o texto primeiro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun salvarNota() {
        val novoTexto = etContent.text.toString()
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val noteAtual = db.noteDao().getNoteById(noteId)
            noteAtual?.let {
                it.content = novoTexto
                db.noteDao().update(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteDetailActivity, "Memória sincronizada!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}