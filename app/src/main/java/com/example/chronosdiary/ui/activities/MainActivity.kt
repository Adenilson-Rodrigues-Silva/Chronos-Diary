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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.ui.adapters.NoteAdapter
import com.example.chronosdiary.R
import com.example.chronosdiary.data.AppDatabase
import com.example.chronosdiary.data.model.NoteDao
import com.example.chronosdiary.utils.VoiceHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor



class MainActivity : AppCompatActivity() {



    private lateinit var lottieMic: LottieAnimationView



    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()

    private lateinit var voiceHelper: VoiceHelper

    private lateinit var fabMicPrincipal: FloatingActionButton // Usaremos o FAB que está no seu XML
    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var rvNotes: RecyclerView
    // Mude de: private lateinit var lottieMic: LottieAnimationView
// Para:


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAudioPermission()

        // AQUI O ERRO MORRE: Vinculamos o ID que existe no activity_main.xml
        rvNotes = findViewById(R.id.recycler_notes)
        fabMicPrincipal = findViewById(R.id.fab_mic) // Usando o ID fab_mic do seu XML

        // Inicializar Banco
        database = AppDatabase.getDatabase(this)
        noteDao = database.noteDao()

        // Configurar o Adapter
        noteAdapter = NoteAdapter(listOf())
        rvNotes.adapter = noteAdapter
        rvNotes.layoutManager = LinearLayoutManager(this)

        // IMPORTANTE: O clique do botão da tela principal abre o seu Google Cloud BottomSheet
        fabMicPrincipal.setOnClickListener {
            showVoiceSheet()
        }

        // Biometria
        if (checkDeviceCanAuthenticate()) {
            setupBiometric()
        } else {
            showChronosLogs()
        }

        // Configuração do Clique do Microfone (Ajustado para o seu BottomSheet)
      //  lottieMic.setOnClickListener {
        //    showVoiceSheet()
       // }
    }

    private fun showChronosLogs() {
        runOnUiThread {
            rvNotes.visibility = View.VISIBLE
            // Se você mudou o nome da variável para fabMicPrincipal:
            fabMicPrincipal.visibility = View.VISIBLE
            refreshNotes()
        }
    }

    // Criar essa função separada facilita sua vida
    private fun refreshNotes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notasSalvas = noteDao.getAllNotes()
            runOnUiThread {
                noteAdapter.updateNotes(notasSalvas)
                // Se a lista estiver vazia agora, ela continuará vazia até você carregar
            }
        }
    }

    // Função auxiliar para deixar o onCreate mais limpo
    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
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


    private fun addNewLog(textoFinal: String) {
        // 1. Criamos a data formatada para o diário
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dataFormatada = sdf.format(java.util.Date())

        // 2. Criamos o objeto Note (usando o seu modelo Note.kt)
        val novaNota = Note(date = dataFormatada, content = textoFinal)

        // 3. Salvamos no banco em uma Coroutine (para não travar a tela)
        lifecycleScope.launch(Dispatchers.IO) {
            noteDao.insert(novaNota) // Salva no banco!

            // 4. Após salvar, atualizamos a lista na tela
            val listaAtualizada = noteDao.getAllNotes()

            runOnUiThread {
                noteAdapter.updateNotes(listaAtualizada)
                // Opcional: faz a lista rolar para o topo para ver a nota nova
                rvNotes.scrollToPosition(0)
            }
        }
    }

    private fun showVoiceSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_voice_capture, null)
        dialog.setContentView(view)

        val lottieVisualizer = view.findViewById<LottieAnimationView>(R.id.lottie_visualizer)
        // Buscamos o mic da VIEW do BottomSheet
        val lottieMicCloud = view.findViewById<LottieAnimationView>(R.id.lottieMic)

        voiceHelper = VoiceHelper(
            this,
            onResult = { textoFinal ->
                runOnUiThread {
                    if (textoFinal.isNotEmpty()) {
                        addNewLog(textoFinal)
                    }
                    lottieVisualizer.cancelAnimation()
                    view.postDelayed({ dialog.dismiss() }, 1000)
                }
            },
            onStatusChange = { status ->
                runOnUiThread {
                    when (status) {
                        "LISTENING" -> {
                            lottieVisualizer.visibility = View.VISIBLE

                            // AQUI: Devolvendo a escala que você gostava
                            lottieVisualizer.scaleX = 0.5f
                            lottieVisualizer.scaleY = 0.8f
                            lottieVisualizer.playAnimation()

                            // AQUI: Usamos lottieMicCloud
                            lottieMicCloud.setAnimation(R.raw.mic_verde_test_2)
                            lottieMicCloud.playAnimation()
                        }

                        "PROCESSING" -> {
                            lottieVisualizer.cancelAnimation()
                            lottieVisualizer.visibility = View.GONE

                            // AQUI: Usamos lottieMicCloud
                            lottieMicCloud.setAnimation(R.raw.save_note)
                            lottieMicCloud.playAnimation()
                            lottieMicCloud.repeatCount = 0 // ou loop(false)
                        }
                    }
                }
            }
        )

        // AQUI: O clique deve ser na lottieMicCloud
        lottieMicCloud.setOnClickListener {
            if (::voiceHelper.isInitialized) {
                voiceHelper.stopAndSend()
            }
        }

        dialog.show()
        view.postDelayed({ voiceHelper.startListening() }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Isso garante que, se você fechar o app, o microfone e as tarefas
        // em segundo plano do VoiceHelper sejam encerradas imediatamente.
        if (::voiceHelper.isInitialized) {
            voiceHelper.destroy()
        }
    }

    override fun onResume() {
        super.onResume()
        // Toda vez que você voltar para esta tela, ela busca os dados novos
        updateFeed()
    }

    private fun updateFeed() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val updatedNotes = db.noteDao().getAllNotes()

            withContext(Dispatchers.Main) {
                // Aqui passamos a lista nova para o seu Adapter
                noteAdapter.updateNotes(updatedNotes)
            }
        }
    }



}