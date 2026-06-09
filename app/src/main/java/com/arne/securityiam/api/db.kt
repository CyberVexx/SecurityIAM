package com.arne.securityiam.api

import java.sql.Connection
import java.sql.DriverManager

class db {
    companion object {
        fun getConnection() : Connection? {
            val dbUrl = "jdbc:mysql://10.0.2.2:3306/i_a_m_db"
            val dbUser = "root"
            val dbPassword = ""

            return try {
                Class.forName("com.mysql.jdbc.Driver")
                val conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
                println("=== DB CONNECTION SUCCESS: $conn ===")
                conn
            } catch (e: Exception) {
                println("=== DB CONNECTION FAILED: ${e::class.java.simpleName}: ${e.message} ===")
                e.printStackTrace()
                null
            }
        }
    }
}