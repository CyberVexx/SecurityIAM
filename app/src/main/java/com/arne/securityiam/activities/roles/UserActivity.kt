package com.arne.securityiam.activities.roles

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.arne.securityiam.R
import com.arne.securityiam.activities.auth.LoginActivity
import com.arne.securityiam.utils.SessionManager

class UserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_user)

        val sessionManager = SessionManager(this)

        // 1. Get user data from Intent
        val personId = intent.getIntExtra("PERSON_ID", -1)
        val name = intent.getStringExtra("NAME") ?: "User"
        val role = intent.getStringExtra("ROLE") ?: "unknown"
        val email = intent.getStringExtra("EMAIL") ?: "N/A"
        val birthDate = intent.getStringExtra("BIRTH_DATE") ?: "N/A"
        val address = intent.getStringExtra("ADDRESS") ?: "N/A"

        // 2. Initialize and set UI views
        findViewById<TextView>(R.id.tv_user_name).text = "Name: $name"
        findViewById<TextView>(R.id.tv_user_role).text = "Role: ${role.replaceFirstChar { it.uppercase() }}"
        findViewById<TextView>(R.id.tv_user_email).text = "Email: $email"
        findViewById<TextView>(R.id.tv_user_birthdate).text = "Birth Date: $birthDate"
        findViewById<TextView>(R.id.tv_user_address).text = "Address: $address"

        findViewById<TextView>(R.id.tv_welcome_title).text = "Welcome, $name"

        // 3. Handle Logout Button
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 4. Handle Dashboard Navigation
        findViewById<Button>(R.id.btn_go_dashboard).setOnClickListener {
            val targetActivity = when (role.lowercase()) {
                "doctor" -> DoctorActivity::class.java
                "nurse" -> NurseActivity::class.java
                "patient" -> PatientActivity::class.java
                else -> null
            }

            targetActivity?.let {
                val intent = Intent(this, it)
                intent.putExtra("PERSON_ID", personId)
                intent.putExtra("NAME", name)
                intent.putExtra("ROLE", role)
                intent.putExtra("EMAIL", email)
                intent.putExtra("BIRTH_DATE", birthDate)
                intent.putExtra("ADDRESS", address)
                startActivity(intent)
            }
        }
    }
}