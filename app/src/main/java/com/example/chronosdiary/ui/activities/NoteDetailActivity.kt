package com.example.chronosdiary.ui.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteDetailActivity : AppCompatActivity() {

    // 1. PROPRIEDADES DA CLASSE (Apenas declarações)
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("CHRONOS", "Imagem selecionada: $uri")
            inserirImagemNoTexto(uri)
        } else {
            Log.d("CHRONOS", "Nenhuma imagem selecionada")
        }
    }

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
    private var isFormattingProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // 2. INICIALIZAÇÃO DOS COMPONENTES (Apenas após o setContentView)
        etContent = findViewById(R.id.et_detail_content)
        btnBack = findViewById(R.id.btn_back)
        tvDate = findViewById(R.id.tv_detail_date)
        imgSelectedMood = findViewById(R.id.img_selected_mood)
        barFormatting = findViewById(R.id.bar_formatting)
        barMoodSelection = findViewById(R.id.bar_mood_selection)

        // Botões da Barra Inferior
        val btnToggleFormat = findViewById<ImageButton>(R.id.btn_toggle_format)
        val btnToggleMood = findViewById<ImageButton>(R.id.btn_toggle_mood)
        val btnSave = findViewById<ImageButton>(R.id.fab_save_changes)
        val btnMarker = findViewById<ImageButton>(R.id.btn_text_color)
        val btnAddImage = findViewById<ImageButton>(R.id.btn_add_image)
        val btnMic = findViewById<ImageButton>(R.id.btn_detail_mic)

        // Botões de Formatação (Menu Flutuante)
        val btnBold = findViewById<ImageButton>(R.id.format_bold)
        val btnItalic = findViewById<ImageButton>(R.id.format_italic)
        val btnUnderline = findViewById<ImageButton>(R.id.format_underline)

        // Botão de Opções (Topo)
        val btnMenu = findViewById<ImageButton>(R.id.btn_more_options)

        // 3. CARREGAR DADOS DA NOTA
        val intentId = intent.getLongExtra("NOTE_ID", -1L)
        noteId = if (intentId == -1L) intent.getIntExtra("NOTE_ID", -1).toLong() else intentId

        if (noteId != -1L) {
            loadNoteData(noteId)
        }

        // 4. CONFIGURAÇÃO DE CLIQUES (LISTENERS)

        btnBack.setOnClickListener { finish() }

        btnToggleFormat.setOnClickListener {
            barFormatting.visibility = if (barFormatting.visibility == View.GONE) View.VISIBLE else View.GONE
            barMoodSelection.visibility = View.GONE
        }

        btnToggleMood.setOnClickListener {
            barMoodSelection.visibility = if (barMoodSelection.visibility == View.GONE) View.VISIBLE else View.GONE
            barFormatting.visibility = View.GONE
        }

        btnSave.setOnClickListener { salvarNota() }

        // Abrir Galeria
        btnAddImage.setOnClickListener {
            Toast.makeText(this, "Acessando Galeria...", Toast.LENGTH_SHORT).show()
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Menu de Opções (Três Pontinhos)
        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_note_detail, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        Toast.makeText(this, "Deletar em breve...", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        configurarCliquesDosEmojis()

        // 5. LÓGICA DE FORMATAÇÃO (BOLD, ITALIC, UNDERLINE, MARKER)
        btnBold.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                val spannable = etContent.text
                val spans = spannable.getSpans(start, end, StyleSpan::class.java)
                var removido = false
                for (span in spans) {
                    if (span.style == Typeface.BOLD) {
                        spannable.removeSpan(span)
                        removido = true
                    }
                }
                if (!removido) spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                isBoldActive = !isBoldActive
                Toast.makeText(this, "Negrito ${if (isBoldActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
        }

        btnItalic.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                val spannable = etContent.text
                val spans = spannable.getSpans(start, end, StyleSpan::class.java)
                var temItalico = false
                for (span in spans) {
                    if (span.style == Typeface.ITALIC) {
                        spannable.removeSpan(span)
                        temItalico = true
                    }
                }
                if (!temItalico) spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                isItalicActive = !isItalicActive
                Toast.makeText(this, "Itálico ${if (isItalicActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
        }

        btnUnderline.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                val spannable = etContent.text
                val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)
                if (spans.isNotEmpty()) {
                    for (span in spans) spannable.removeSpan(span)
                } else {
                    spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                isUnderlineActive = !isUnderlineActive
                Toast.makeText(this, "Sublinhado ${if (isUnderlineActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
        }

        btnMarker.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            val editable = etContent.text
            if (start >= 0 && end >= 0 && start != end) {
                var finalStart = minOf(start, end)
                var finalEnd = maxOf(start, end)
                val existingSpans = editable.getSpans(finalStart, finalEnd, BackgroundColorSpan::class.java)
                var removido = false
                if (existingSpans.isNotEmpty()) {
                    for (span in existingSpans) {
                        val s = editable.getSpanStart(span)
                        val e = editable.getSpanEnd(span)
                        if (finalStart >= s && finalEnd <= e) {
                            editable.removeSpan(span)
                            removido = true
                        } else {
                            finalStart = minOf(finalStart, s)
                            finalEnd = maxOf(finalEnd, e)
                            editable.removeSpan(span)
                        }
                    }
                }
                if (!removido) {
                    val corNeon = Color.argb(120, 0, 255, 204)
                    editable.setSpan(BackgroundColorSpan(corNeon), finalStart, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        // 6. TEXTWATCHER PARA DIGITAÇÃO EM TEMPO REAL
        etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormattingProgrammatically || s == null) return
                val selectionStart = etContent.selectionStart
                if (selectionStart > 0 && selectionStart == etContent.selectionEnd) {
                    try {
                        val p = selectionStart - 1
                        if (isBoldActive) s.setSpan(StyleSpan(Typeface.BOLD), p, selectionStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        if (isItalicActive) s.setSpan(StyleSpan(Typeface.ITALIC), p, selectionStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        if (isUnderlineActive) s.setSpan(UnderlineSpan(), p, selectionStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } catch (e: Exception) {}
                }
            }
        })
    }

    // --- FUNÇÕES AUXILIARES ---

    private fun inserirImagemNoTexto(uri: Uri) {
        try {
            val drawable = contentResolver.openInputStream(uri)?.use { inputStream ->
                Drawable.createFromStream(inputStream, uri.toString())
            }

            drawable?.let {
                val width = 400
                val aspectRatio = it.intrinsicHeight.toFloat() / it.intrinsicWidth.toFloat()
                val height = (width * aspectRatio).toInt()
                it.setBounds(0, 0, width, height)

                val spannable = etContent.text
                val selectionStart = etContent.selectionStart

                val imageSpan = ImageSpan(it, ImageSpan.ALIGN_BASELINE)
                spannable.insert(selectionStart, "\n \n")
                spannable.setSpan(imageSpan, selectionStart + 1, selectionStart + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                etContent.setSelection(selectionStart + 3)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarCliquesDosEmojis() {
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
            }
        }
    }

    private fun loadNoteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val note = db.noteDao().getNoteById(id)
            withContext(Dispatchers.Main) {
                note?.let {
                    val textoHtml = Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY)
                    val spannable = android.text.SpannableStringBuilder(textoHtml)

                    val spans = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
                    for (span in spans) {
                        val start = spannable.getSpanStart(span)
                        val end = spannable.getSpanEnd(span)
                        val corSuave = Color.argb(120, Color.red(span.backgroundColor), Color.green(span.backgroundColor), Color.blue(span.backgroundColor))
                        spannable.removeSpan(span)
                        spannable.setSpan(BackgroundColorSpan(corSuave), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    etContent.setText(spannable)
                    tvDate.text = it.date
                }
            }
        }
    }

    private fun salvarNota() {
        val textoParaSalvar = Html.toHtml(etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val noteAtual = db.noteDao().getNoteById(noteId)
            noteAtual?.let {
                it.content = textoParaSalvar
                db.noteDao().update(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteDetailActivity, "Sincronizado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}