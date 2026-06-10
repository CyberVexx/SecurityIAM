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
import com.arne.securityiam.activities.roles.DoctorActivity
import com.arne.securityiam.activities.roles.NurseActivity
import com.arne.securityiam.activities.roles.PatientActivity
import com.arne.securityiam.api.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    val targetActivity = when (user.role) {
                        "doctor" -> DoctorActivity::class.java
                        "nurse" -> NurseActivity::class.java
                        "patient" -> PatientActivity::class.java
                        else -> {
                            Toast.makeText(this@LoginActivity, "Unknown role: ${user.role}", Toast.LENGTH_LONG).show()
                            return@onSuccess
                        }
                    }

                    val intent = Intent(this@LoginActivity, targetActivity)
                    intent.putExtra("PERSON_ID", user.id)
                    intent.putExtra("NAME", user.name)
                    startActivity(intent)
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@LoginActivity, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
