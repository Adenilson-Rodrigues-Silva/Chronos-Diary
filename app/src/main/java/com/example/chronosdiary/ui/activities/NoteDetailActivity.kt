package com.example.chronosdiary.ui.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
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

    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false


    private var isFormattingProgrammatically = false // teste para ver se funciona


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

        // Botões de formatação
        val btnBold = findViewById<ImageButton>(R.id.format_bold)
        val btnItalic = findViewById<ImageButton>(R.id.format_italic)
        val btnUnderline = findViewById<ImageButton>(R.id.format_underline)

        // 2. RECUPERAR ID E CARREGAR DADOS (Antes dos Listeners)
        val intentId = intent.getLongExtra("NOTE_ID", -1L)
        noteId = if (intentId == -1L) intent.getIntExtra("NOTE_ID", -1).toLong() else intentId

        if (noteId != -1L) {
            loadNoteData(noteId)
        }

        // 3. MENUS (TOGGLE)
        btnToggleFormat.setOnClickListener {
            barFormatting.visibility =
                if (barFormatting.visibility == View.GONE) View.VISIBLE else View.GONE
            barMoodSelection.visibility = View.GONE
        }
        btnToggleMood.setOnClickListener {
            barMoodSelection.visibility =
                if (barMoodSelection.visibility == View.GONE) View.VISIBLE else View.GONE
            barFormatting.visibility = View.GONE
        }
        btnBack.setOnClickListener { finish() }

        // 4. EMOJIS
        configurarCliquesDosEmojis()

        // =================================================================
        // 5. LÓGICA DE FORMATAÇÃO (BLINDADA CONTRA CRASHES)
        // =================================================================

        // --- NEGRITO ---
        // --- NEGRITO (Ajustado para Seleção) ---
        btnBold.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd

            if (start != end) {
                try {
                    val spannable = etContent.text
                    // Remove qualquer estilo de negrito que já exista na seleção para não "empilhar"
                    val spans = spannable.getSpans(start, end, StyleSpan::class.java)
                    var removido = false
                    for (span in spans) {
                        if (span.style == Typeface.BOLD) {
                            spannable.removeSpan(span)
                            removido = true
                        }
                    }
                    // Se não removeu nada, aplica o novo
                    if (!removido) {
                        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                isBoldActive = !isBoldActive
                Toast.makeText(this, "Negrito ${if (isBoldActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
        }

        // --- ITÁLICO ---
        btnItalic.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd

            if (start != end) {
                try {
                    val spannable = etContent.text
                    val spans = spannable.getSpans(start, end, StyleSpan::class.java)
                    var temItalico = false
                    for (span in spans) {
                        if (span.style == Typeface.ITALIC) {
                            spannable.removeSpan(span)
                            temItalico = true
                        }
                    }
                    if (!temItalico) {
                        spannable.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                isItalicActive = !isItalicActive
                val status = if (isItalicActive) "ATIVADO" else "DESATIVADO"
                Toast.makeText(this, "Itálico $status", Toast.LENGTH_SHORT).show()
            }
        }

        // --- SUBLINHADO ---
        btnUnderline.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd

            if (start != end) {
                try {
                    val spannable = etContent.text
                    val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)
                    if (spans.isNotEmpty()) {
                        for (span in spans) spannable.removeSpan(span)
                    } else {
                        spannable.setSpan(
                            UnderlineSpan(),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                isUnderlineActive = !isUnderlineActive
                val status = if (isUnderlineActive) "ATIVADO" else "DESATIVADO"
                Toast.makeText(this, "Sublinhado $status", Toast.LENGTH_SHORT).show()
            }
        }

        // --- MARCADOR (SOMENTE SELEÇÃO) ---
        // --- MARCADOR (Ajustado para funcionar sempre) ---
        // --- MARCADOR (A Versão Unificadora) ---
        // --- MARCADOR (A Versão Unificadora) ---
        btnMarker.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            val editable = etContent.text

            if (start >= 0 && end >= 0 && start != end) {
                try {
                    var finalStart = minOf(start, end)
                    var finalEnd = maxOf(start, end)

                    // 1. Busca spans na área
                    val existingSpans = editable.getSpans(finalStart, finalEnd, BackgroundColorSpan::class.java)

                    // 2. LÓGICA DA BORRACHA:
                    // Se o usuário selecionou EXATAMENTE um trecho que já está marcado, nós removemos.
                    var removido = false
                    if (existingSpans.isNotEmpty()) {
                        for (span in existingSpans) {
                            val s = editable.getSpanStart(span)
                            val e = editable.getSpanEnd(span)

                            // Se a seleção está dentro ou é igual ao span existente, removemos
                            if (finalStart >= s && finalEnd <= e) {
                                editable.removeSpan(span)
                                removido = true
                            } else {
                                // Se for uma união, expandimos os limites (sua lógica que funcionou!)
                                finalStart = minOf(finalStart, s)
                                finalEnd = maxOf(finalEnd, e)
                                editable.removeSpan(span)
                            }
                        }
                    }

                    // 3. Se não foi uma ação de "apenas remover", aplicamos a nova marcação unificada
                    if (!removido) {
                        val corNeon = Color.argb(120, 0, 255, 204)
                        editable.setSpan(
                            BackgroundColorSpan(corNeon),
                            finalStart,
                            finalEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Selecione o texto primeiro", Toast.LENGTH_SHORT).show()
            }
        }





        // 6. SALVAR
        btnSave.setOnClickListener { salvarNota() }

        // 7. VIGIA DE TEXTO (TEXTWATCHER) - Última coisa a ser configurada
        etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormattingProgrammatically) return
                if (s == null) return

                val selectionStart = etContent.selectionStart

                // Só processa se o cursor estiver parado (digitando) e não selecionando
                // E só se tiver texto antes do cursor (selectionStart > 0)
                if (selectionStart > 0 && selectionStart == etContent.selectionEnd) {
                    try {
                        val p = selectionStart - 1

                        // Aplica estilos apenas se a variável estiver ativa
                        if (isBoldActive) {
                            s.setSpan(
                                StyleSpan(Typeface.BOLD),
                                p,
                                selectionStart,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        if (isItalicActive) {
                            s.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                p,
                                selectionStart,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        if (isUnderlineActive) {
                            s.setSpan(
                                UnderlineSpan(),
                                p,
                                selectionStart,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    } catch (e: Exception) {
                        // Se der erro de índice, o app não fecha, apenas ignora
                    }
                }
            }
        })
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
                imgSelectedMood.imageTintList =
                    ColorStateList.valueOf(Color.parseColor(data.second))
                barMoodSelection.visibility = View.GONE
                Toast.makeText(this, "Humor atualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNoteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val note = db.noteDao().getNoteById(id) // Aqui o objeto se chama 'note'

            withContext(Dispatchers.Main) {
                note?.let {
                    // 1. Converte o HTML do banco
                    val textoHtml = Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY)
                    val spannable = android.text.SpannableStringBuilder(textoHtml)

                    // 2. Suaviza as cores estouradas
                    val spans = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
                    for (span in spans) {
                        val start = spannable.getSpanStart(span)
                        val end = spannable.getSpanEnd(span)
                        val corOriginal = span.backgroundColor

                        val corSuave = Color.argb(120, Color.red(corOriginal), Color.green(corOriginal), Color.blue(corOriginal))

                        spannable.removeSpan(span)
                        spannable.setSpan(BackgroundColorSpan(corSuave), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    // 3. Aplica o texto corrigido no EditText
                    etContent.setText(spannable)
                    tvDate.text = it.date
                }
            }
        }
    }



    private fun salvarNota() {
        // 1. Transformamos o conteúdo colorido em HTML
        val textoParaSalvar = Html.toHtml(etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val noteAtual = db.noteDao().getNoteById(noteId)

            noteAtual?.let {
                // 2. AQUI ESTÁ O SEGREDO: Salvar a versão com HTML, não o toString()
                it.content = textoParaSalvar

                db.noteDao().update(it)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        "Memória sincronizada!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }




}