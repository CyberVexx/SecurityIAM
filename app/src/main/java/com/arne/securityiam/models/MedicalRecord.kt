package com.arne.securityiam.models

data class MedicalRecord(
    val id: Int,
    val patientName: String,
    val doctorName: String,
    val diagnosis: String,
    val treatmentDate: String
)