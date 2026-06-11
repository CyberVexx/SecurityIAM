package com.arne.securityiam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arne.securityiam.R
import com.arne.securityiam.models.MedicalRecord

class RecordAdapter(private val records: List<MedicalRecord>) :
    RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPatientName: TextView = view.findViewById(R.id.tv_patient_name)
        val tvDate: TextView = view.findViewById(R.id.tv_appointment_date)
        val tvDiagnosis: TextView = view.findViewById(R.id.tv_appointment_reason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.tvPatientName.text = "Patient: ${record.patientName}"
        holder.tvDate.text = "Date: ${record.treatmentDate}"
        holder.tvDiagnosis.text = "Diagnosis: ${record.diagnosis}"
    }

    override fun getItemCount() = records.size
}