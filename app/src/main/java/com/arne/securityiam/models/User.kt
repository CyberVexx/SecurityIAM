package com.arne.securityiam.models

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)
