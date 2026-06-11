package com.arne.securityiam.activities.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arne.securityiam.R
import com.arne.securityiam.api.db
import com.arne.securityiam.utils.PasswordValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var et_forgot_email: EditText
    private lateinit var et_forgot_new_password: EditText
    private lateinit var et_forgot_confirm_password: EditText
    private lateinit var btn_reset_password: Button
    private lateinit var tv_back_to_login: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        et_forgot_email = findViewById(R.id.et_forgot_email)
        et_forgot_new_password = findViewById(R.id.et_forgot_new_password)
        et_forgot_confirm_password = findViewById(R.id.et_forgot_confirm_password)
        btn_reset_password = findViewById(R.id.btn_reset_password)
        tv_back_to_login = findViewById(R.id.tv_back_to_login)

        btn_reset_password.setOnClickListener {
            resetPassword()
        }

        tv_back_to_login.setOnClickListener {
            finish()
        }
    }

    private fun resetPassword() {
        val email = et_forgot_email.text.toString().trim()
        val newPassword = et_forgot_new_password.text.toString()
        val confirmPassword = et_forgot_confirm_password.text.toString()

        if (email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        PasswordValidator.validate(newPassword).onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            return
        }

        btn_reset_password.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                db.resetPassword(email, newPassword)
            }

            btn_reset_password.isEnabled = true

            result
                .onSuccess {
                    Toast.makeText(this@ForgotPasswordActivity, "Password reset successful", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@ForgotPasswordActivity, "Reset failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
