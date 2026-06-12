package com.arne.securityiam.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arne.securityiam.R
import com.arne.securityiam.activities.auth.LoginActivity
import com.arne.securityiam.activities.roles.UserActivity
import com.arne.securityiam.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val sessionManager = SessionManager(this)

        lifecycleScope.launch {
            delay(3000)
            if (sessionManager.isLoggedIn()) {
                val user = sessionManager.getUserSession()
                if (user != null) {
                    val intent = Intent(this@SplashActivity, UserActivity::class.java)
                    intent.putExtra("PERSON_ID", user.id)
                    intent.putExtra("NAME", user.name)
                    intent.putExtra("ROLE", user.role)
                    intent.putExtra("EMAIL", user.email)
                    intent.putExtra("BIRTH_DATE", user.birthDate)
                    intent.putExtra("ADDRESS", user.address)
                    startActivity(intent)
                } else {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
