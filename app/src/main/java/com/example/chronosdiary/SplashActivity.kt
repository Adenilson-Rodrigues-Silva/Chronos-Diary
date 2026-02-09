package com.example.chronosdiary


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esconde a barra de cima para ficar tela cheia
        supportActionBar?.hide()

        // Espera 3000 milisegundos (3 segundos) e muda de tela
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Destrói a Splash para o usuário não voltar pra ela no botão "voltar"
        }, 3000)
    }
}