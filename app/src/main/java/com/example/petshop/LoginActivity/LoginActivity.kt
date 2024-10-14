package com.example.petshop.LoginActivity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petshop.RegisterActivity.RegisterActivity
import com.example.petshop.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            // Lógica para fazer login
        }

        binding.btnRegister.setOnClickListener {
            // Navegar para a tela de registro
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
