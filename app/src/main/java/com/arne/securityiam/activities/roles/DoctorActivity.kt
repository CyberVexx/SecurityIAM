package com.arne.securityiam.activities.roles

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arne.securityiam.R
import com.arne.securityiam.adapters.RecordAdapter
import com.arne.securityiam.api.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DoctorActivity : AppCompatActivity() {
    private lateinit var rvRecords: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_doctor)

        val doctorId = intent.getIntExtra("PERSON_ID", -1)

        // Back Button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        rvRecords = findViewById(R.id.rv_appointments)
        rvRecords.layoutManager = LinearLayoutManager(this)

        if (doctorId != -1) {
            loadRecords(doctorId)
        }
    }

    private fun loadRecords(doctorId: Int) {
        lifecycleScope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    db.getRecordsForDoctor(doctorId)
                }
                
                if (records.isEmpty()) {
                    Toast.makeText(this@DoctorActivity, "No medical records found", Toast.LENGTH_SHORT).show()
                } else {
                    rvRecords.adapter = RecordAdapter(records)
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorActivity, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}