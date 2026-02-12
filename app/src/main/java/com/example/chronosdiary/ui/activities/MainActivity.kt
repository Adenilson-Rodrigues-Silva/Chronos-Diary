package com.example.chronosdiary.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.ui.adapters.NoteAdapter
import com.example.chronosdiary.R
import com.example.chronosdiary.utils.VoiceHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()

    private lateinit var voiceHelper: VoiceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pedir permissão de áudio
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        // 1. Checar biometria
        if (checkDeviceCanAuthenticate()) {
            setupBiometric()
        }
    }

    private fun checkDeviceCanAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> return true
            else -> {
                Toast.makeText(
                    this,
                    "Biometria necessária para acessar o Chronos.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showChronosLogs()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("> SYSTEM_LOCKED")
            .setSubtitle("VERIFY_IDENTITY_SCAN_REQUIRED")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showChronosLogs() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_notes)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_note)

        if (notesList.isEmpty()) {
            notesList.add(Note(1, "10 FEV 2026", "Sistema Chronos Inicializado."))
        }

        noteAdapter = NoteAdapter(notesList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        recyclerView.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        fab.setOnClickListener {
            showVoiceSheet()
        }
    }

    private fun addNewLog(text: String) {


        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("pt", "BR"))
        val currentDate = sdf.format(java.util.Date()).uppercase()

        val newNote = Note(notesList.size + 1, currentDate, text)
        notesList.add(0, newNote)
        // ... resto do código


        notesList.add(0, newNote)
        noteAdapter.notifyItemInserted(0)
        findViewById<RecyclerView>(R.id.recycler_notes).scrollToPosition(0)
    }

    private fun showVoiceSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)

        // 1. Inflar a view primeiro (A ordem aqui é vital!)
        val view = layoutInflater.inflate(R.layout.layout_voice_capture, null)
        dialog.setContentView(view)

        // 2. Referenciar os componentes APÓS inflar a view
        val partialTv = view.findViewById<android.widget.TextView>(R.id.partial_text_view)
        val statusTv = view.findViewById<android.widget.TextView>(R.id.status_text)
        val btnCheck = view.findViewById<android.widget.ImageButton>(R.id.btn_finish_voice)
        val lottieVisualizer = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_visualizer)

        // 3. Configurar o VoiceHelper
        voiceHelper = VoiceHelper(
            this,
            onResult = { textoFinal ->
                runOnUiThread {
                    if (textoFinal.isNotEmpty()) {
                        partialTv.text = textoFinal
                        addNewLog(textoFinal)
                    } else {
                        Toast.makeText(this, "Nenhum áudio reconhecido", Toast.LENGTH_SHORT).show()
                    }

                    // Limpeza visual ao fechar
                    lottieVisualizer.cancelAnimation()
                    dialog.dismiss()
                }
            },
            onStatusChange = { status ->
                runOnUiThread {
                    when (status) {
                        "LISTENING" -> {
                            runOnUiThread {
                                lottieVisualizer.visibility = View.VISIBLE
                                lottieVisualizer.scaleX = 1.5f // Aumenta a largura em 50% além do padrão
                                lottieVisualizer.scaleY = 1.5f // Aumenta um pouco a altura para dar impacto
                                lottieVisualizer.playAnimation()

                                statusTv.text = "> SYSTEM_ACTIVE: LISTENING..."
                                statusTv.setTextColor(ContextCompat.getColor(this, R.color.neon_green))

                                // Inicia as ondas sonoras
                                lottieVisualizer.visibility = View.VISIBLE
                                lottieVisualizer.playAnimation()

                                // Configura o botão
                                btnCheck.setImageResource(R.drawable.ic_mic)
                                startPulseAnimation(btnCheck, 1.3f)
                            }
                        }

                        "PROCESSING" -> {
                            runOnUiThread {
                                statusTv.text = "> PROCESSING AUDIO..."
                                statusTv.setTextColor(android.graphics.Color.CYAN)

                                // Esconde as ondas para dar espaço ao processamento
                                lottieVisualizer.cancelAnimation()
                                lottieVisualizer.visibility = View.GONE

                                // Troca o ícone para EDIT (Caneta)
                                btnCheck.clearAnimation()
                                btnCheck.setImageResource(R.drawable.ic_edit)
                            }
                        }

                        "DONE" -> {
                            statusTv.text = "> COMPLETE"
                            statusTv.setTextColor(android.graphics.Color.GREEN)
                            btnCheck.clearAnimation()
                        }

                        "ERROR" -> {
                            statusTv.text = "> SYSTEM_ERROR: CHECK INTERNET"
                            statusTv.setTextColor(android.graphics.Color.RED)
                            lottieVisualizer.visibility = View.GONE
                        }
                    }
                }
            }
        )

        // 4. Configurar o clique do botão
        btnCheck.setOnClickListener {
            // Verifica se a variável foi inicializada para evitar o crash
            if (::voiceHelper.isInitialized) {
                btnCheck.setImageResource(R.drawable.ic_edit)
                voiceHelper.stopAndSend()
            }
        }

        dialog.show()

        // 5. Iniciar o microfone com um pequeno delay para o sistema respirar
        view.postDelayed({
            voiceHelper.startListening()
        }, 300)
    }
    override fun onDestroy() {
        super.onDestroy()
        // Isso garante que, se você fechar o app, o microfone e as tarefas
        // em segundo plano do VoiceHelper sejam encerradas imediatamente.
        if (::voiceHelper.isInitialized) {
            voiceHelper.destroy()
        }
    }

    private fun startPulseAnimation(view: View, scale: Float) {
        val scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, scale, 1f)
        val scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, scale, 1f)

        scaleX.repeatCount = android.animation.ValueAnimator.INFINITE
        scaleY.repeatCount = android.animation.ValueAnimator.INFINITE

        android.animation.AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000
            start()
        }
    }

}