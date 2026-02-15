package com.example.chronosdiary.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // 1. Inicialização do Adapter (Evita o erro de Lateinit)
            noteAdapter = NoteAdapter(listOf())

            // 2. Vincular Views
            rvNotes = findViewById(R.id.recycler_notes)
            fabMicPrincipal = findViewById(R.id.fab_mic)

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
        val dataAlvo = sdf.format(data.time)

        lifecycleScope.launch(Dispatchers.IO) {
            val todas = noteDao.getAllNotes()
            // O segredo do Feed: Usamos contains para ignorar a hora exata
            val filtradas = todas.filter { it.date.contains(dataAlvo) }

            withContext(Dispatchers.Main) {
                // Atualiza o adapter com a lista filtrada
                noteAdapter.updateNotes(filtradas)

                val layoutVazio = findViewById<LinearLayout>(R.id.layout_vazio)
                val rvNotes = findViewById<RecyclerView>(R.id.recycler_notes)

                if (filtradas.isEmpty()) {
                    // Se não tem notas: Mostra Astronauta, Esconde Lista
                    layoutVazio?.visibility = View.VISIBLE
                    rvNotes?.visibility = View.GONE

                    // Força a animação do astronauta a rodar
                    val lottieEmpty = findViewById<LottieAnimationView>(R.id.lottie_empty)
                    lottieEmpty?.playAnimation()
                } else {
                    // Se tem notas: Esconde Astronauta, Mostra Lista
                    layoutVazio?.visibility = View.GONE
                    rvNotes?.visibility = View.VISIBLE
                }
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
        if (::voiceHelper.isInitialized) voiceHelper.destroy()
    }

    override fun onResume() {
        super.onResume()
        // Isso força o app a recarregar as notas do banco toda vez que você volta à tela principal
        updateFeed()
    }
}