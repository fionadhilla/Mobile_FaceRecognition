package com.example.Mobile_FaceRecognition.presentation.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.Mobile_FaceRecognition.R

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnClikLogin = findViewById<Button>(R.id.btn_login)
        btnClikLogin.setOnClickListener {
            val intent = Intent(this@LoginActivity, com.example.Mobile_FaceRecognition.presentation.view.MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}