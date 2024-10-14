package com.example.projeto_integrador_4

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseAuthUserCollisionException
//import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var btnVoltar: Button
    private lateinit var btnCadastrar: Button
    private lateinit var etNome: TextInputLayout
    private lateinit var etEmail: TextInputLayout
    private lateinit var etIdade: TextInputLayout
    private lateinit var etCPF: TextInputLayout
    private lateinit var etCelular: TextInputLayout
    private lateinit var etSenha: TextInputLayout

//    private lateinit var db: FirebaseFirestore
//    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

    //    auth = FirebaseAuth.getInstance()
    //   db = FirebaseFirestore.getInstance()

        btnVoltar = findViewById(R.id.btnVoltar)
        btnCadastrar = findViewById(R.id.btnCadastrar)

        etNome = findViewById(R.id.etNome)
        etEmail = findViewById(R.id.etEmail)
        etIdade = findViewById(R.id.etIdade)
        etCPF = findViewById(R.id.etCPF)
        etCelular = findViewById(R.id.etCelular)
        etSenha = findViewById(R.id.etSenha)

        btnVoltar.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCadastrar.setOnClickListener {
            val sNome = etNome.editText?.text.toString()
            val sEmail = etEmail.editText?.text.toString()
            val sIdade = etIdade.editText?.text.toString()
            val sCPF = etCPF.editText?.text.toString()
            val sCelular = etCelular.editText?.text.toString()
            val sSenha = etSenha.editText?.text.toString()
//
//            if (sNome.isNotEmpty() && sEmail.isNotEmpty() && sIdade.isNotEmpty() &&
//                sCPF.isNotEmpty() && sCelular.isNotEmpty() && sSenha.isNotEmpty()) {
//                if (!sEmail.contains("@admin.com")){
//                    auth.createUserWithEmailAndPassword(sEmail, sSenha)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                val userId = auth.currentUser?.uid
//                                val user = hashMapOf(
//                                    "name" to sNome,
//                                    "email" to sEmail,
//                                    "idade" to sIdade,
//                                    "cpf" to sCPF,
//                                    "celular" to sCelular
//                                )
//
//                                if (userId != null) {
//                                    db.collection("users").document(userId)
//                                        .set(user)
//                                        .addOnSuccessListener {
//                                            Log.d(
//                                                "CadastroActivity",
//                                                "DocumentSnapshot added with ID: $userId"
//                                            )
//                                            sendEmailVerification()
//                                        }
//                                        .addOnFailureListener { e ->
//                                            Log.w("CadastroActivity", "Error adding document", e)
//                                            Toast.makeText(
//                                                baseContext,
//                                                "Erro ao salvar dados do usuário.",
//                                                Toast.LENGTH_SHORT
//                                            ).show()
//                                        }
//                                }
//                            } else {
//                                try {
//                                    throw task.exception!!
//                                } catch (e: FirebaseAuthUserCollisionException) {
//                                    Log.w("CadastroActivity", "createUserWithEmail:failure", e)
//                                    Toast.makeText(
//                                        baseContext,
//                                        "Email já cadastrado por outro usuário.",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                } catch (e: Exception) {
//                                    Log.w("CadastroActivity", "createUserWithEmail:failure", e)
//                                    Toast.makeText(
//                                        baseContext,
//                                        "Falha na autenticação.",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                            }
//                        }
//                }else{
//                    Toast.makeText(
//                        baseContext,
//                        "Email Invalido.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } else {
//                Toast.makeText(baseContext, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun sendEmailVerification() {
//        val user = auth.currentUser
//        user?.sendEmailVerification()
//            ?.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    Log.d("CadastroActivity", "Email de verificação enviado.")
//                    Toast.makeText(this, "Email de verificação enviado. Verifique seu email e faça login.", Toast.LENGTH_LONG).show()
//                    val intent = Intent(this, LoginActivity::class.java)
//                    startActivity(intent)
//                    finish()
//                } else {
//                    Log.e("CadastroActivity", "Erro ao enviar email de verificação.", task.exception)
//                    Toast.makeText(this, "Erro ao enviar email de verificação.", Toast.LENGTH_SHORT).show()
//                }
//            }
    }
}