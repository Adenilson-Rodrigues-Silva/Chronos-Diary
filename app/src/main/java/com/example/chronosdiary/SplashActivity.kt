package com.example.chronosdiary

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esconde a barra de cima para ficar full screen
        supportActionBar?.hide()

        val logo = findViewById<ImageView>(R.id.logo_chronos)
        val title = findViewById<TextView>(R.id.text_title)
        val textToType = "CHRONOS DIARY"

        // 1. Inicia a animação de pulso no logo
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        logo.startAnimation(pulseAnim)

        // 2. Efeito Terminal para o Título
        title.text = ""
        var index = 0
        val handler = Handler(Looper.getMainLooper())

        val runner = object : Runnable {
            override fun run() {
                if (index <= textToType.length) {
                    title.text = textToType.substring(0, index)
                    index++
                    handler.postDelayed(this, 150) // Velocidade da digitação
                }
            }
        }
        handler.post(runner)

        // 3. Timer para ir para a próxima tela
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 4000) // 4.0 segundos para dar tempo de ver tudo
    }
}