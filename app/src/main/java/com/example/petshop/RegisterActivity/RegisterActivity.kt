package com.example.petshop.RegisterActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petshop.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            // Lógica para registrar o usuário
        }
    }
}
