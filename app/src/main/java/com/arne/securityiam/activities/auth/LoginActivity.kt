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
import com.arne.securityiam.activities.roles.UserActivity
import com.arne.securityiam.api.db
import com.arne.securityiam.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var et_login_username: EditText
    private lateinit var et_login_password: EditText
    private lateinit var btn_login: Button
    private lateinit var tv_register: TextView
    private lateinit var tv_forgot_password: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        et_login_username = findViewById(R.id.et_login_username)
        et_login_password = findViewById(R.id.et_login_password)
        btn_login = findViewById(R.id.btn_login)
        tv_register = findViewById(R.id.tv_register)
        tv_forgot_password = findViewById(R.id.tv_forgot_password)

        btn_login.setOnClickListener {
            val name = et_login_username.text.toString().trim()
            val password = et_login_password.text.toString().trim()

            if (name.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(name, password)
        }

        tv_register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tv_forgot_password.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun login(name: String, password: String) {
        btn_login.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                db.login(name, password)
            }

            btn_login.isEnabled = true

            result
                .onSuccess { user ->
                    sessionManager.saveUserSession(user)
                    val intent = Intent(this@LoginActivity, UserActivity::class.java)
                    intent.putExtra("PERSON_ID", user.id)
                    intent.putExtra("NAME", user.name)
                    intent.putExtra("ROLE", user.role)
                    intent.putExtra("EMAIL", user.email)
                    intent.putExtra("BIRTH_DATE", user.birthDate)
                    intent.putExtra("ADDRESS", user.address)
                    startActivity(intent)
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@LoginActivity, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
