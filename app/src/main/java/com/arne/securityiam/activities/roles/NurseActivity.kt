package com.arne.securityiam.activities.roles

import android.os.Bundle
import android.widget.ImageButton
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

class NurseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_nurse)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_nurse_appointments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                db.getAllRecords()
            }
            recyclerView.adapter = RecordAdapter(records)
        }
    }
}