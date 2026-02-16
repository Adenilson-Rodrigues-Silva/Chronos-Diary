package com.example.chronosdiary.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.data.model.NoteDao
import com.example.chronosdiary.ui.adapters.NoteAdapter
import com.example.chronosdiary.utils.VoiceHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var rvNotes: RecyclerView
    private lateinit var fabMicPrincipal: FloatingActionButton
    private lateinit var noteAdapter: NoteAdapter

    // Database
    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao

    // Biometrics
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Voice Helper
    private lateinit var voiceHelper: VoiceHelper

    // Animations
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_open) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_close) }

    // FAB Sub-buttons
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMicSub: FloatingActionButton
    private lateinit var fabWrite: FloatingActionButton

    private var isMenuExpanded = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // 1. Inicialização do Adapter (Evita o erro de Lateinit)
            noteAdapter = NoteAdapter(listOf())

            // 2. Vincular Views
            rvNotes = findViewById(R.id.recycler_notes)
            fabMicPrincipal = findViewById(R.id.fab_main)

            // 3. Configurar RecyclerView
            rvNotes.adapter = noteAdapter
            rvNotes.layoutManager = LinearLayoutManager(this)

            // 4. Inicializar Banco de Dados
            database = AppDatabase.getDatabase(this)
            noteDao = database.noteDao()

            // 5. Configurar Botão do Microfone
            fabMicPrincipal.setOnClickListener {
                checkAudioPermission()
                showVoiceSheet()
            }

            // 6. Iniciar Fluxo de Segurança (Biometria)
            rvNotes.postDelayed({
                if (checkDeviceCanAuthenticate()) setupBiometric()
                else showChronosLogs()
            }, 500)

            // 7. Montar Calendário
            configurarCalendarioHorizontal()

        } catch (e: Exception) {
            Log.e("CHRONOS_FATAL", "Erro no onCreate: ${e.message}")
        }

        // Inicializar os botões do XML
        fabMain = findViewById(R.id.fab_main)
        fabMicSub = findViewById(R.id.fab_mic_sub)
        fabWrite = findViewById(R.id.fab_write)

        fabMain.setOnClickListener {
            // Se o menu NÃO estiver expandido, significa que vamos abrir agora
            if (!isMenuExpanded) {
                dispararPixelsEnergia(fabMain) // Chama a explosão de pixels
            }

            alternarMenuFab() // Executa a rotação e o salto dos botões que já fizemos
        }

        // Dentro do onCreate, onde configuramos os cliques:

        fabWrite.setOnClickListener {
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("NOTE_ID", -1L)      // Indica que é uma nota nova
            intent.putExtra("START_VOICE", false) // Apenas abrir para digitar
            startActivity(intent)
            alternarMenuFab()
        }

        fabMicSub.setOnClickListener {
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("NOTE_ID", -1L)      // Indica que é uma nota nova
            intent.putExtra("START_VOICE", true)  // Abrir e JÁ COMEÇAR A OUVIR
            startActivity(intent)
            alternarMenuFab()
        }
    }



    private fun showChronosLogs() {
        runOnUiThread {
            rvNotes.visibility = View.VISIBLE
            fabMicPrincipal.visibility = View.VISIBLE
            updateFeed() // Carrega as notas do dia
        }
    }

    private fun updateFeed() {
        val hoje = Calendar.getInstance()
        filtrarNotasPorData(hoje)
    }

    private fun checkDeviceCanAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> {
                Toast.makeText(this, "Segurança Biométrica necessária", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showChronosLogs()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) finish()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("CHRONOS_LOCKED")
            .setSubtitle("VERIFICAÇÃO DE IDENTIDADE REQUERIDA")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun filtrarNotasPorData(data: Calendar) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dataAlvo = sdf.format(data.time).trim()

        Log.d("CHRONOS_FEED", "Filtrando notas para o dia: $dataAlvo")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todas = noteDao.getAllNotes()
                // Filtra notas que tenham EXATAMENTE essa data
                val filtradas = todas.filter { it.date.trim() == dataAlvo }

                withContext(Dispatchers.Main) {
                    noteAdapter.updateNotes(filtradas)

                    val layoutVazio = findViewById<LinearLayout>(R.id.layout_vazio)
                    val rvNotes = findViewById<RecyclerView>(R.id.recycler_notes)

                    if (filtradas.isEmpty()) {
                        layoutVazio?.visibility = View.VISIBLE
                        rvNotes?.visibility = View.GONE
                        findViewById<LottieAnimationView>(R.id.lottie_empty)?.playAnimation()
                    } else {
                        layoutVazio?.visibility = View.GONE
                        rvNotes?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("CHRONOS_ERROR", "Erro ao filtrar: ${e.message}")
            }
        }
    }

    private fun configurarCalendarioHorizontal() {
        val container = findViewById<LinearLayout>(R.id.calendar_container) ?: return
        val inflater = LayoutInflater.from(this)
        val hoje = Calendar.getInstance()
        val cal = Calendar.getInstance()

        container.removeAllViews()

        for (i in 0 until 14) {
            val view = inflater.inflate(R.layout.item_calendar_day, container, false)
            val diaAtual = cal.clone() as Calendar

            val txtNome = view.findViewById<TextView>(R.id.text_day_name)
            val txtNum = view.findViewById<TextView>(R.id.text_day_number)

            txtNome.text = SimpleDateFormat("EEE", Locale("pt", "BR")).format(diaAtual.time).uppercase()
            txtNum.text = SimpleDateFormat("dd", Locale.getDefault()).format(diaAtual.time)

            if (isMesmoDia(hoje, diaAtual)) {
                txtNum.setBackgroundResource(R.drawable.bg_dia_selecionado)
                atualizarSubtitulo(diaAtual)
            }

            view.setOnClickListener {
                atualizarSelecaoCalendario(view, container)
                atualizarSubtitulo(diaAtual)
                filtrarNotasPorData(diaAtual)
            }

            container.addView(view)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    private fun isMesmoDia(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

    private fun atualizarSubtitulo(data: Calendar) {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
        findViewById<TextView>(R.id.text_subheader).text = sdf.format(data.time).replaceFirstChar { it.uppercase() }
    }

    private fun atualizarSelecaoCalendario(selecionada: View, container: LinearLayout) {
        for (i in 0 until container.childCount) {
            container.getChildAt(i).findViewById<TextView>(R.id.text_day_number).setBackgroundResource(R.drawable.bg_dia_vazio)
        }
        selecionada.findViewById<TextView>(R.id.text_day_number).setBackgroundResource(R.drawable.bg_dia_selecionado)
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun showVoiceSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_voice_capture, null)
        dialog.setContentView(view)

        val lottieWaves = view.findViewById<LottieAnimationView>(R.id.lottie_visualizer)
        val lottieMic = view.findViewById<LottieAnimationView>(R.id.lottieMic)

        voiceHelper = VoiceHelper(this,
            onResult = { texto ->
                if (texto.isNotEmpty()) addNewLog(texto)
                dialog.dismiss()
            },
            onStatusChange = { status ->
                runOnUiThread {
                    when (status) {
                        "LISTENING" -> {
                            lottieWaves?.visibility = View.VISIBLE
                            lottieWaves?.setAnimation(R.raw.wave_test_3)
                            lottieWaves?.playAnimation()

                            lottieMic?.setAnimation(R.raw.mic_verde_test_2)
                            lottieMic?.playAnimation()
                        }
                        "PROCESSING" -> {
                            lottieWaves?.visibility = View.GONE
                            lottieMic?.setAnimation(R.raw.save_note)
                            lottieMic?.playAnimation()
                            lottieMic?.repeatCount = 0
                        }
                    }
                }
            }
        )

        lottieMic?.setOnClickListener { voiceHelper.stopAndSend() }
        dialog.show()
        voiceHelper.startListening()
    }

    private fun addNewLog(texto: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nota = Note(date = sdf.format(Date()), content = texto)

        lifecycleScope.launch(Dispatchers.IO) {
            noteDao.insert(nota)
            withContext(Dispatchers.Main) {
                updateFeed()
                rvNotes.scrollToPosition(0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // A trava 'isInitialized' evita que o app quebre se você fechar a tela
        // antes de clicar no microfone pela primeira vez.
        if (::voiceHelper.isInitialized) {
            voiceHelper.destroy()
        }
    }

    override fun onResume() {
        super.onResume()
        // Isso força o app a recarregar as notas do banco toda vez que você volta à tela principal
        updateFeed()

    }

    private fun alternarMenuFab() {
        definirVisibilidade(isMenuExpanded)
        definirAnimacao(isMenuExpanded)
        definirInteratividade(isMenuExpanded)
        isMenuExpanded = !isMenuExpanded
    }

    private fun definirVisibilidade(expanded: Boolean) {
        if (!expanded) {
            fabMicSub.visibility = View.VISIBLE
            fabWrite.visibility = View.VISIBLE
        } else {
            // Usamos View.INVISIBLE em vez de GONE para não quebrar a animação de descida
            fabMicSub.visibility = View.INVISIBLE
            fabWrite.visibility = View.INVISIBLE
        }
    }

    private fun definirAnimacao(expanded: Boolean) {
        if (!expanded) {
            // ABRIR
            fabMain.startAnimation(rotateOpen)

            // Mic: Sobe com efeito de mola
            fabMicSub.animate()
                .translationY(-200f)
                .alpha(1f) // Garante que apareça
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .setDuration(400)
                .start()

            // Caneta: Sobe mais alto com efeito de mola
            fabWrite.animate()
                .translationY(-400f)
                .alpha(1f)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .setDuration(500) // Um pouquinho mais lenta para dar sensação de peso
                .start()

        } else {
            // FECHAR
            fabMain.startAnimation(rotateClose)

            // Mic: Volta exatamente para o centro do botão +
            fabMicSub.animate()
                .translationY(0f)
                .alpha(0f) // Vai sumindo enquanto desce
                .setDuration(300)
                .start()

            // Caneta: Volta exatamente para o centro do botão +
            fabWrite.animate()
                .translationY(0f)
                .alpha(0f)
                .setDuration(300)
                .start()
        }
    }

    private fun definirInteratividade(expanded: Boolean) {
        if (!expanded) {
            fabMicSub.isClickable = true
            fabWrite.isClickable = true
        } else {
            fabMicSub.isClickable = false
            fabWrite.isClickable = false
        }
    }
    // Classe simples para controlar cada pixel da explosão
    data class Particle(
        val view: View,
        val velocityX: Float,
        val velocityY: Float
    )

    private fun dispararPixelsEnergia(anchorView: View) {
        val container = anchorView.parent as ViewGroup
        val particleCount = 20 // Aumentei um pouco para ficar mais rico
        val particles = mutableListOf<Particle>()

        for (i in 0 until particleCount) {
            val pixel = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(12, 12)

                // Cria um círculo Cyan neon
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#00FFCC"))
                }

                // Posição inicial no centro do botão
                x = anchorView.x + anchorView.width / 2
                y = anchorView.y + anchorView.height / 2
                elevation = 20f
                alpha = 0.7f // Um pouco mais transparente para parecer luz
            }

            container.addView(pixel)

            // Direção aleatória (Explosão 360 graus)
            val vX = (Math.random() * 15 - 7.5).toFloat()
            val vY = (Math.random() * 15 - 7.5).toFloat()

            particles.add(Particle(pixel, vX, vY))
        }

        // Animação dos pixels flutuando
        particles.forEach { p ->
            p.view.animate()
                .translationXBy(p.velocityX * 40) // Distância menor para ser mais sutil
                .translationYBy(p.velocityY * 40)
                .alpha(0f)          // Desaparece suavemente
                .scaleX(0.3f)       // Encolhe enquanto flutua
                .scaleY(0.3f)
                .setDuration(1500)  // Mais tempo = mais devagar
                .setInterpolator(android.view.animation.DecelerateInterpolator()) // Efeito de freio
                .withEndAction { container.removeView(p.view) }
                .start()
        }
    }

}