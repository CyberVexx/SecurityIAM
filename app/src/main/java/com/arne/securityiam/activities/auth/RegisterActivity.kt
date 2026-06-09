package com.arne.securityiam.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import at.favre.lib.crypto.bcrypt.BCrypt
import com.arne.securityiam.R

class RegisterActivity : AppCompatActivity() {
//    private lateinit var et_register_name: EditText
//    private lateinit var et_register_email: EditText
//    private lateinit var et_register_password: EditText
//    private lateinit var et_register_confirm_password: EditText
//    private lateinit var btn_register: Button
    private lateinit var tv_login: TextView


//    val plainPassword = passwordInput.text.toString()
//    val hashedPassword = BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tv_login = findViewById(R.id.tv_login)

        tv_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}