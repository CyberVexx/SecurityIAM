package com.arne.securityiam.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arne.securityiam.R
import com.arne.securityiam.api.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var et_register_name: EditText
    private lateinit var et_register_email: EditText
    private lateinit var et_register_password: EditText
    private lateinit var et_register_confirm_password: EditText
    private lateinit var btn_register: Button
    private lateinit var tv_login: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        et_register_name = findViewById(R.id.et_register_name)
        et_register_email = findViewById(R.id.et_register_email)
        et_register_password = findViewById(R.id.et_register_password)
        et_register_confirm_password = findViewById(R.id.et_register_confirm_password)
        btn_register = findViewById(R.id.btn_register)
        tv_login = findViewById(R.id.tv_login)

        btn_register.setOnClickListener {
            register()
        }

        tv_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun register() {
        val name = et_register_name.text.toString().trim()
        val email = et_register_email.text.toString().trim()
        val password = et_register_password.text.toString()
        val confirmPassword = et_register_confirm_password.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        btn_register.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                db.registerPatient(name, email, password)
            }

            btn_register.isEnabled = true

            result
                .onSuccess {
                    Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@RegisterActivity, "Registration failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
