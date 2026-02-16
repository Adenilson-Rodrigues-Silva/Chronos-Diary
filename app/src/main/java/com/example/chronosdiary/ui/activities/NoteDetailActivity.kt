package com.example.chronosdiary.ui.activities


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.utils.VoiceHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class NoteDetailActivity : AppCompatActivity() {

    // 1. PROPRIEDADES E SELEÇÃO DE MÍDIA
    // 1. Onde você pede a "chave permanente" para o Android
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                // Pede permissão permanente para ler este arquivo específico
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)

                // Agora sim, insere no texto
                inserirImagemNoTexto(uri)
            } catch (e: Exception) {
                Log.e("CHRONOS", "Erro ao persistir permissão: ${e.message}")
                inserirImagemNoTexto(uri) // Tenta inserir mesmo se falhar a persistência
            }
        }
    }

   // private lateinit var voiceLayout: View

    private lateinit var etContent: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDate: TextView
    private lateinit var imgSelectedMood: ImageView
    private lateinit var barFormatting: CardView
    private lateinit var barMoodSelection: CardView
    private lateinit var layoutMenuFormatacao: View // ou LinearLayout/CardView

    private var noteId: Long = -1L
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false
    private var isFormattingProgrammatically = false

    // Componentes de UI

    private lateinit var lottieMic: LottieAnimationView
    private lateinit var lottieWave: LottieAnimationView
    private lateinit var voiceLayout: LinearLayout

    // Voice Helper
    private lateinit var voiceHelper: VoiceHelper

    // Estas variáveis representam os componentes da gaveta que acabamos de criar

    private lateinit var tvStatusVoz: TextView

    // O seu ajudante de voz (Helper)

    // ... restante das suas variáveis (database, etc)





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        inicializarComponentes()
        carregarDadosDaNota()
        configurarBotoesPrincipais()
        configurarMenuDeOpcoes()
        configurarFerramentasDeTexto()
        configurarCliquesDosEmojis()


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // NOVO MÉTODOs
      //  verificarAcaoInicial()



// GARANTIA: Força ele a ficar invisível e escondido no início


        // --- CONFIGURAÇÃO DA MEIA TELA (BOTTOM SHEET) ---
        // 1. Referencia o layout que agora tem o ID 'voice_sheet'
        val voiceSheet = findViewById<LinearLayout>(R.id.voice_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(voiceSheet)

        voiceSheet.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // 2. Começa escondido (lá embaixo)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // 3. O botão que abre a gravação (usando o ID correto do XML)
        val btnMicAbreGaveta = findViewById<ImageButton>(R.id.btn_detail_mic)
        btnMicAbreGaveta.setOnClickListener {
            // Quando clica manualmente, nós mostramos a gaveta
            voiceSheet.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            dispararEntradaPorVoz()
        }
        // ... dentro do onCreate logo após configurar o voiceSheet e o behavior ...
        verificarAcaoInicial(voiceSheet, bottomSheetBehavior)

    }

    private fun inicializarComponentes() {
        // Componentes do Editor
        etContent = findViewById(R.id.et_detail_content)
        btnBack = findViewById(R.id.btn_back)
        tvDate = findViewById(R.id.tv_detail_date)
        imgSelectedMood = findViewById(R.id.img_selected_mood)
        barFormatting = findViewById(R.id.bar_formatting)
        barMoodSelection = findViewById(R.id.bar_mood_selection)

        // Mapeando a GAVETA DE VOZ (Include)
       // val voiceSheet = findViewById<LinearLayout>(R.id.voice_sheet)
        //voiceLayout = findViewById(R.id.layout_voice_capture)
       // lottieMic = findViewById(R.id.lottieMic)
        //lottieWave = findViewById(R.id.lottie_visualizer)
        //tvStatusVoz = findViewById(R.id.tv_status_voz)

        // CORREÇÃO DOS IDS DA GAVETA
        voiceLayout = findViewById(R.id.voice_sheet) // Use o ID novo 'voice_sheet'
        lottieMic = findViewById(R.id.lottieMic)
        lottieWave = findViewById(R.id.lottie_visualizer)
        tvStatusVoz = findViewById(R.id.tv_status_voz)



        // Inicializando o VoiceHelper
        voiceHelper = VoiceHelper(
            context = this,
            onResult = { textoTranscritp ->
                runOnUiThread {
                    // 1. Insere o texto transcrito
                    val textoAntigo = etContent.text.toString()
                    val novoTexto = if (textoAntigo.isEmpty()) textoTranscritp else "$textoAntigo $textoTranscritp"
                    etContent.setText(novoTexto)
                    etContent.setSelection(etContent.text.length)

                    // 2. Fecha a gaveta e reseta animações
                    lottieMic.cancelAnimation()
                    lottieWave.cancelAnimation()
                    voiceLayout.visibility = View.GONE

                    // Volta o mic para a animação original para a próxima vez
                    lottieMic.setAnimation(R.raw.mic_verde_test_2)
                }
            },
            onStatusChange = { status ->
                runOnUiThread {
                    when(status) {
                        "LISTENING" -> {
                            tvStatusVoz.text = "ESCUTANDO MEMÓRIA..."
                            lottieWave.visibility = View.VISIBLE
                        }
                        "PROCESSING" -> {
                            tvStatusVoz.text = "PROCESSANDO NO CHRONOS..."
                            // TROCA PARA A ANIMAÇÃO DE SAVE QUE VOCÊ PEDIU
                            lottieMic.setAnimation(R.raw.save_note)
                            lottieMic.playAnimation()
                            lottieWave.visibility = View.GONE // Esconde as ondas
                        }
                        "ERROR" -> {
                            tvStatusVoz.text = "ERRO DE CONEXÃO"
                            lottieMic.cancelAnimation()
                            voiceLayout.postDelayed({ voiceLayout.visibility = View.GONE }, 2000)
                        }
                    }
                }
            }
        )
    }

    private fun carregarDadosDaNota() {
        val intentId = intent.getLongExtra("NOTE_ID", -1L)
        noteId = if (intentId == -1L) intent.getIntExtra("NOTE_ID", -1).toLong() else intentId
        if (noteId != -1L) {
            loadNoteData(noteId)
        }
    }

    private fun configurarBotoesPrincipais() {
        // Botões da Barra Inferior
        val btnToggleFormat = findViewById<ImageButton>(R.id.btn_toggle_format)
        val btnToggleMood = findViewById<ImageButton>(R.id.btn_toggle_mood)
        val btnDetailMic = findViewById<ImageButton>(R.id.btn_detail_mic)
        val btnAddImage = findViewById<ImageButton>(R.id.btn_add_image)
        val btnSave = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_save_changes)

        // 1. Abrir/Fechar Menu de Formatação (B)
        btnToggleFormat.setOnClickListener {
            val estaVisivel = barFormatting.visibility == View.VISIBLE
            barFormatting.visibility = if (estaVisivel) View.GONE else View.VISIBLE
            barMoodSelection.visibility = View.GONE // Fecha o outro se estiver aberto
        }

        // 2. Abrir/Fechar Menu de Emojis
        btnToggleMood.setOnClickListener {
            val estaVisivel = barMoodSelection.visibility == View.VISIBLE
            barMoodSelection.visibility = if (estaVisivel) View.GONE else View.VISIBLE
            barFormatting.visibility = View.GONE // Fecha o outro se estiver aberto
        }

        // 3. Clique no Mic da Barra Inferior (Abre a Gaveta)
        btnDetailMic.setOnClickListener {
            dispararEntradaPorVoz()
        }

        // 4. Clique no Mic GRANDE (Para de gravar e processa)
        lottieMic.setOnClickListener {
            voiceHelper.stopAndSend()
        }

        btnAddImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSave.setOnClickListener {
            salvarNota()
        }
        btnBack.setOnClickListener {
            finish() // ISSO faz o botão voltar funcionar
        }
    }

    private fun configurarMenuDeOpcoes() {
        val btnMenu = findViewById<ImageButton>(R.id.btn_more_options)
        val neonColor = Color.parseColor("#00FFCC")

        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_note_detail, popup.menu)

            try {
                val method = popup.menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(popup.menu, true)
            } catch (e: Exception) {}

            for (i in 0 until popup.menu.size()) {
                popup.menu.getItem(i).icon?.setTint(neonColor)
            }

            ////TUDO DO MENU SUSPENÇÃO É AQUI
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        confirmarExclusao()
                        Toast.makeText(this, "Ação de apagar: Amanhã!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_info -> { // O ID que você colocou no XML do menu
                        exibirDetalhesDaNota()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun configurarFerramentasDeTexto() {
        val btnBold = findViewById<ImageButton>(R.id.format_bold)
        val btnItalic = findViewById<ImageButton>(R.id.format_italic) ?: return // O ?: return evita crash se o ID sumir
        val btnUnderline = findViewById<ImageButton>(R.id.format_underline) ?: return

        val btnMarker = findViewById<ImageButton>(R.id.btn_text_color)

        val colorCyan = ColorStateList.valueOf(Color.parseColor("#00FFCC"))
        val colorWhite = ColorStateList.valueOf(Color.WHITE)

        // 1. NEGRITO
        btnBold.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                aplicarEstiloSelecao(Typeface.BOLD)
            } else {
                isBoldActive = !isBoldActive
                btnBold.imageTintList = if (isBoldActive) colorCyan else colorWhite
                if (!isBoldActive) removerEstilosDaPosicaoAtual(StyleSpan::class.java, Typeface.BOLD)
            }
            barFormatting.visibility = View.GONE
        }

        // 2. ITÁLICO
        btnItalic.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                aplicarEstiloSelecao(Typeface.ITALIC)
            } else {
                isItalicActive = !isItalicActive
                btnItalic.imageTintList = if (isItalicActive) colorCyan else colorWhite
                if (!isItalicActive) removerEstilosDaPosicaoAtual(StyleSpan::class.java, Typeface.ITALIC)
            }
            barFormatting.visibility = View.GONE
        }

        // 3. SUBLINHADO
        btnUnderline.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                aplicarEstiloSelecaoUnderline()
            } else {
                isUnderlineActive = !isUnderlineActive
                btnUnderline.imageTintList = if (isUnderlineActive) colorCyan else colorWhite
                if (!isUnderlineActive) removerEstilosDaPosicaoAtual(UnderlineSpan::class.java)
            }
            barFormatting.visibility = View.GONE
        }

        // 4. MARCADOR NEON (AQUI ESTÁ ELE!)
        btnMarker.setOnClickListener {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                aplicarMarcadorNeon()
            } else {
                Toast.makeText(this, "Selecione o texto primeiro", Toast.LENGTH_SHORT).show()
            }
            barFormatting.visibility = View.GONE
        }

        // --- TEXT WATCHER ---
        etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormattingProgrammatically || s == null || s.isEmpty()) return
                val selectionStart = etContent.selectionStart
                if (selectionStart > 0 && selectionStart == etContent.selectionEnd) {
                    try {
                        val p = selectionStart - 1
                        if (isBoldActive) s.setSpan(StyleSpan(Typeface.BOLD), p, selectionStart, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                        if (isItalicActive) s.setSpan(StyleSpan(Typeface.ITALIC), p, selectionStart, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                        if (isUnderlineActive) s.setSpan(UnderlineSpan(), p, selectionStart, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        })
    }

// --- FUNÇÕES AUXILIARES

    private fun <T> removerEstilosDaPosicaoAtual(classe: Class<T>, styleType: Int? = null) {
        val selection = etContent.selectionStart
        val spannable = etContent.text
        val spans = spannable.getSpans(selection, selection, classe)
        for (span in spans) {
            if (styleType == null || (span is StyleSpan && span.style == styleType)) {
                val start = spannable.getSpanStart(span)
                spannable.setSpan(span, start, selection, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    // Função auxiliar para o underline não dar erro de índice
    private fun aplicarEstiloSelecaoUnderline() {
        try {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            val spannable = etContent.text
            val finalStart = minOf(start, end)
            val finalEnd = maxOf(start, end)

            val spans = spannable.getSpans(finalStart, finalEnd, UnderlineSpan::class.java)
            if (spans.isNotEmpty()) {
                for (span in spans) spannable.removeSpan(span)
            } else {
                spannable.setSpan(UnderlineSpan(), finalStart, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Função auxiliar para o marcador não crashar
    private fun aplicarMarcadorNeon() {
        try {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            if (start != end) {
                val editable = etContent.text
                val finalStart = minOf(start, end)
                val finalEnd = maxOf(start, end)
                val spans = editable.getSpans(finalStart, finalEnd, BackgroundColorSpan::class.java)
                for (span in spans) editable.removeSpan(span)
                val corNeon = Color.argb(120, 0, 255, 204)
                editable.setSpan(BackgroundColorSpan(corNeon), finalStart, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- FUNÇÃO AUXILIAR (Adicione logo abaixo da função acima) ---
// Isso evita crash e duplicação de código
    private fun aplicarEstiloSelecao(style: Int) {
        try {
            val start = etContent.selectionStart
            val end = etContent.selectionEnd
            val spannable = etContent.text

            if (start == -1 || end == -1 || start == end) return

            val finalStart = minOf(start, end)
            val finalEnd = maxOf(start, end)

            val spans = spannable.getSpans(finalStart, finalEnd, StyleSpan::class.java)
            var spanRemovido = false
            for (span in spans) {
                if (span.style == style) {
                    spannable.removeSpan(span)
                    spanRemovido = true
                }
            }
            if (!spanRemovido) {
                spannable.setSpan(StyleSpan(style), finalStart, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (e: Exception) {
            Log.e("CHRONOS", "Erro aplicarEstilo: ${e.message}")
        }
    }

    // --- MANTIVE AS DEMAIS FUNÇÕES QUE VOCÊ JÁ TINHA ---


    //// Processa a imagem escolhida da galeria e a insere como um ImageSpan dentro do corpo do texto
    private fun inserirImagemNoTexto(uri: Uri) {
        try {
            // 1. Criar o HTML manual com a URI da imagem
            val imageTag = "<img src=\"$uri\">"
            val selectionStart = etContent.selectionStart

            // 2. Inserir a tag no texto e re-processar o HTML para exibir a imagem na hora
            etContent.text.insert(selectionStart, Html.fromHtml(imageTag, Html.FROM_HTML_MODE_LEGACY, { source ->
                val d = Drawable.createFromStream(contentResolver.openInputStream(uri), source)
                d?.let {
                    val width = 400
                    val aspectRatio = it.intrinsicHeight.toFloat() / it.intrinsicWidth.toFloat()
                    it.setBounds(0, 0, width, (width * aspectRatio).toInt())
                }
                d
            }, null))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // Gerencia a seleção de humor: troca o ícone principal e a cor neon de acordo com o emoji clicado
    private fun configurarCliquesDosEmojis() {
        // Criamos o mapa garantindo que o segundo valor seja uma String de cor
        val moods = mapOf(
            R.id.select_mood_very_sad to Pair(R.drawable.ic_mod_very_sad, "#FF4444"),
            R.id.select_mood_sad to Pair(R.drawable.ic_mod_sad, "#FF8800"),
            // R.id.select_mood_stress -> Removi pois causava erro se não estivesse no XML
            R.id.select_mood_neutral to Pair(R.drawable.ic_mod_neutral, "#00FFCC"),
            R.id.select_mood_happy to Pair(R.drawable.ic_mod_happy, "#AAFF00"),
            R.id.select_mood_very_happy to Pair(R.drawable.ic_mod_very_happy, "#00FF00")
            // R.id.select_mood_very_angry -> Removi pois causava erro se não estivesse no XML
        )

        moods.forEach { (id, data) ->
            val btn = findViewById<ImageButton>(id)
            btn?.setOnClickListener {
                imgSelectedMood.setImageResource(data.first)
                imgSelectedMood.imageTintList = ColorStateList.valueOf(Color.parseColor(data.second))
                barMoodSelection.visibility = View.GONE
            }
        }
    }


    // Busca a nota no banco de dados local (Room) usando Coroutines e preenche o editor com o conteúdo salvo
    private fun loadNoteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val note = db.noteDao().getNoteById(id)
            withContext(Dispatchers.Main) {
                note?.let {
                    tvDate.text = it.date

                    val imageGetter = Html.ImageGetter { source ->
                        try {
                            val imageUri = Uri.parse(source)
                            val inputStream = contentResolver.openInputStream(imageUri)
                            val drawable = Drawable.createFromStream(inputStream, source)
                            drawable?.let { d ->
                                val width = 600
                                val ratio = d.intrinsicHeight.toFloat() / d.intrinsicWidth.toFloat()
                                d.setBounds(0, 0, width, (width * ratio).toInt())
                            }
                            drawable
                        } catch (e: Exception) { null }
                    }

                    // 1. Carrega o HTML básico
                    val textoBruto = Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
                    val spannable = android.text.SpannableStringBuilder(textoBruto)

                    // 2. CORREÇÃO DO MARCADOR: Procura fundos coloridos e aplica o Neon suave (Alpha 120)
                    val spans = spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
                    for (span in spans) {
                        val start = spannable.getSpanStart(span)
                        val end = spannable.getSpanEnd(span)
                        // Pega a cor que veio (estourada) e coloca 120 de transparência
                        val corOriginal = span.backgroundColor
                        val corNeonSuave = Color.argb(120, Color.red(corOriginal), Color.green(corOriginal), Color.blue(corOriginal))

                        spannable.removeSpan(span)
                        spannable.setSpan(BackgroundColorSpan(corNeonSuave), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    etContent.setText(spannable)
                }
            }
        }
    }

    // Converte o conteúdo formatado do EditText para HTML e sincroniza a atualização no banco de dados
    private fun salvarNota() {
        val textoParaSalvar = Html.toHtml(etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        // PADRONIZAÇÃO: Criamos a data no formato dd/MM/yyyy para o filtro da Main funcionar
        val sdfBanco = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val dataParaOBanco = sdfBanco.format(java.util.Date())

        Log.d("CHRONOS_DEBUG", "Iniciando salvamento. ID atual: $noteId | Data: $dataParaOBanco")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@NoteDetailActivity)

                if (noteId <= 0L) {
                    // CENÁRIO: NOTA NOVA
                    val novaNota = Note(
                        date = dataParaOBanco, // Salva "16/02/2026"
                        content = textoParaSalvar
                    )

                    val novoId = db.noteDao().insert(novaNota)
                    noteId = novoId

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NoteDetailActivity, "Memória Salva!", Toast.LENGTH_SHORT).show()
                        etContent.postDelayed({ finish() }, 300)
                    }
                } else {
                    // CENÁRIO: EDITAR EXISTENTE
                    val noteExistente = db.noteDao().getNoteById(noteId)
                    if (noteExistente != null) {
                        noteExistente.content = textoParaSalvar
                        // Mantemos a data original da nota para ela não "pular" de dia no calendário
                        db.noteDao().update(noteExistente)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NoteDetailActivity, "Sincronizado!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CHRONOS_ERROR", "Erro ao salvar: ${e.message}")
            }
        }
    }


    // Exibe um alerta de confirmação antes de deletar a nota permanentemente
    private fun confirmarExclusao() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Apagar Memória?")
            .setMessage("Esta ação não pode ser desfeita. Deseja excluir permanentemente?")
            .setPositiveButton("Sim, Apagar") { _, _ ->
                deletarNotaDoBanco()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Executa a exclusão no banco de dados Room e fecha a tela
    private fun deletarNotaDoBanco() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@NoteDetailActivity)
            val noteAtual = db.noteDao().getNoteById(noteId)
            noteAtual?.let {
                db.noteDao().delete(it) // Certifique-se que seu DAO tem a função delete
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteDetailActivity, "Memória apagada!", Toast.LENGTH_SHORT).show()
                    finish() // Volta para a tela principal
                }
            }
        }
    }

    // Calcula e exibe estatísticas da nota atual
    private fun exibirDetalhesDaNota() {
        val texto = etContent.text.toString().trim()
        val caracteres = texto.length

        // 1. Cálculo de Palavras e Tempo de Leitura
        val palavras = if (texto.isEmpty()) 0 else texto.split("\\s+".toRegex()).size
        val tempoLeitura = if (palavras > 0) Math.ceil(palavras / 200.0).toInt() else 0

        // 2. Verificar se tem imagens (procura por ImageSpans no texto)
        val temImagens = etContent.text.getSpans(0, etContent.text.length, android.text.style.ImageSpan::class.java).isNotEmpty()
        val statusMidia = if (temImagens) "Contém imagens" else "Apenas texto"

        // 3. Montar o Relatório
        val relatorio = StringBuilder()
        relatorio.append("🆔 Registro: #$noteId\n")
        relatorio.append("📅 Data: ${tvDate.text}\n")
        relatorio.append("📝 Status: $statusMidia\n")
        relatorio.append("----------------------------\n")
        relatorio.append("🔢 Caracteres: $caracteres\n")
        relatorio.append("✍️ Palavras: $palavras\n")
        relatorio.append("⏱️ Leitura estimada: $tempoLeitura min\n")

        // Criar o alerta visual
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Relatório da Memória")
            .setMessage(relatorio.toString())
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun verificarAcaoInicial(sheet: View, behavior: BottomSheetBehavior<LinearLayout>) {
        val noteId = intent.getLongExtra("NOTE_ID", -1L)
        val startVoice = intent.getBooleanExtra("START_VOICE", false)

        if (startVoice && noteId == -1L) {
            sheet.visibility = View.VISIBLE
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dispararEntradaPorVoz()
        } else {
            // Isso aqui mata o problema dos prints 2 e 4
            sheet.visibility = View.GONE
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun dispararEntradaPorVoz() {
        lottieMic.isClickable = true

        // Criamos uma função interna para não repetir código
        val pararGravacao = {
            if (voiceHelper.isListening) {
                Log.d("CHRONOS_TESTE", "Parada solicitada!")
                voiceHelper.stopAndSend()

                tvStatusVoz.text = "PROCESSANDO NO CHRONOS..."
                lottieMic.setAnimation(R.raw.save_note)
                lottieMic.playAnimation()
                lottieWave.visibility = View.GONE
            }
        }


        // Use o View.OnTouchListener para capturar o dedo encostando na tela NA HORA
        lottieMic.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                v.performClick() // Isso tira o aviso amarelo!
                Log.d("CHRONOS_LOG", "TOQUE DETECTADO NO CELL! isListening: ${voiceHelper.isListening}")

                if (voiceHelper.isListening) {
                    // EXECUTAR PARADA
                    tvStatusVoz.text = "PROCESSANDO..."
                    lottieWave.visibility = View.GONE
                    lottieMic.setAnimation(R.raw.save_note)
                    lottieMic.playAnimation()

                    voiceHelper.stopAndSend()
                    return@setOnTouchListener true // Indica que o toque foi consumido
                }
            }
            false
        }


        // 1. Clique no Ícone
        /*lottieMic.setOnClickListener {
            Log.d("CHRONOS_TESTE", "Clique no Mic detectado!")
            if (voiceHelper.isListening) {
                pararGravacao()
            } else {
                voiceHelper.startListening()
                tvStatusVoz.text = "ESCUTANDO MEMÓRIA..."
                lottieWave.visibility = View.VISIBLE
            }
        }*/

        // 2. O PULO DO GATO: Clique no fundo da gaveta (Área escura)
        // Se o clique no Mic falhar, esse aqui captura o toque no celular real
        voiceLayout.setOnClickListener {
            Log.d("CHRONOS_TESTE", "Clique no Layout detectado!")
            pararGravacao()
        }

        // 2. O comando inicial ao abrir a gaveta
        voiceLayout.visibility = View.VISIBLE
        lottieMic.playAnimation()
        lottieWave.visibility = View.VISIBLE
        tvStatusVoz.text = "ESCUTANDO MEMÓRIA..."
        voiceHelper.startListening()
    }

    private fun atualizarStatusVoz(status: String) {
        val tvStatus = findViewById<TextView>(R.id.tv_status_voz) // O TextView que diz "ESCUTANDO..."

        runOnUiThread {
            when (status) {
                "LISTENING" -> tvStatus.text = "ESCUTANDO MEMÓRIA..."
                "PROCESSING" -> {
                    tvStatus.text = "PROCESSANDO NO CHRONOS..."
                    // Opcional: desativar o clique para não enviar duas vezes
                    lottieMic.isClickable = false
                }
                "DONE" -> {
                    lottieMic.isClickable = true
                    voiceLayout.visibility = View.GONE
                }
                "ERROR" -> {
                    tvStatus.text = "ERRO DE CONEXÃO"
                    voiceLayout.postDelayed({ voiceLayout.visibility = View.GONE }, 2000)
                }
            }
        }
    }



}

