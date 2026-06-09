package com.arne.securityiam.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arne.securityiam.R

class LoginActivity : AppCompatActivity() {
    private lateinit var et_login_username: EditText
    private lateinit var et_login_password: EditText
    private lateinit var btn_login: Button
    private lateinit var tv_register: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        et_login_username = findViewById(R.id.et_login_username)
        et_login_password = findViewById(R.id.et_login_password)
        btn_login = findViewById(R.id.btn_login)
        tv_register = findViewById(R.id.tv_register)

        btn_login.setOnClickListener {
            val email = et_login_username.text.toString().trim()
            val password = et_login_password.text.toString().trim()


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }

        tv_register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}