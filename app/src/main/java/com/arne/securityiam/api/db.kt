package com.arne.securityiam.api

import com.arne.securityiam.models.MedicalRecord
import com.arne.securityiam.models.User
import com.arne.securityiam.utils.PasswordHash
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.Calendar

class db {
    companion object {
        fun getConnection() : Connection? {
            val dbUrl = "jdbc:mysql://10.0.2.2:3306/i_a_m_db"
            val dbUser = "root"
            val dbPassword = ""

            return try {
                Class.forName("com.mysql.jdbc.Driver")
                val conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
                conn
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun registerUser(name: String, email: String, plainPassword: String): Result<User> {
            val passwordHash = PasswordHash.hashPassword(plainPassword)

            val connection = getConnection()
                ?: return Result.failure(Exception("Could not connect to the database"))

            return try {
                connection.use { conn ->
                    conn.autoCommit = false

                    try {
                        val insertPersonSql = """
                            INSERT INTO person (name, email, password_hash, is_confirmed, failed_attempts)
                            VALUES (?, ?, ?, 0, 0)
                        """.trimIndent()

                        conn.prepareStatement(insertPersonSql, Statement.RETURN_GENERATED_KEYS).use { personStmt ->
                            personStmt.setString(1, name)
                            personStmt.setString(2, email)
                            personStmt.setString(3, passwordHash)
                            personStmt.executeUpdate()

                            personStmt.generatedKeys.use { keys ->
                                if (!keys.next()) {
                                    throw Exception("Could not create person")
                                }

                                val personId = keys.getInt(1)

                                val insertPatientSql = """
                                    INSERT INTO patient (person_id, registration_date)
                                    VALUES (?, CURDATE())
                                """.trimIndent()

                                conn.prepareStatement(insertPatientSql).use { patientStmt ->
                                    patientStmt.setInt(1, personId)
                                    patientStmt.executeUpdate()
                                }

                                conn.commit()
                                Result.success(User(personId, name, email, "patient"))
                            }
                        }
                    } catch (e: Exception) {
                        conn.rollback()
                        Result.failure(e)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun login(name: String, plainPassword: String): Result<User> {
            val connection = getConnection()
                ?: return Result.failure(Exception("Could not connect to the database"))

            return try {
                connection.use { conn ->
                    val sql = """
                        SELECT
                            p.person_id,
                            p.name,
                            p.email,
                            p.password_hash,
                            p.birth_date,
                            p.address,
                            p.is_confirmed,
                            p.failed_attempts,
                            p.blocked_at,
                            CASE
                                WHEN d.doctor_id IS NOT NULL THEN 'doctor'
                                WHEN n.nurse_id IS NOT NULL THEN 'nurse'
                                WHEN pa.patient_id IS NOT NULL THEN 'patient'
                                ELSE 'unknown'
                            END AS role
                        FROM person p
                        LEFT JOIN doctor d ON d.doctor_id = p.person_id
                        LEFT JOIN nurse n ON n.nurse_id = p.person_id
                        LEFT JOIN patient pa ON pa.person_id = p.person_id
                        WHERE LOWER(p.name) = LOWER(?)
                        LIMIT 1
                    """.trimIndent()

                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, name)

                        stmt.executeQuery().use { rs ->
                            if (!rs.next()) {
                                return Result.failure(Exception("No user found with this name"))
                            }

                            val personId = rs.getInt("person_id")
                            val isConfirmed = rs.getInt("is_confirmed") == 1
                            var failedAttempts = rs.getInt("failed_attempts")
                            val blockedAt = rs.getTimestamp("blocked_at")

                            if (!isConfirmed) {
                                return Result.failure(Exception("Account is not confirmed yet"))
                            }

                            val currentTime = System.currentTimeMillis()
                            if (blockedAt != null) {
                                val lockoutDuration = 1 * 60 * 1000 // 15 minutes lockout
                                val timeElapsed = currentTime - blockedAt.time
                                if (timeElapsed < lockoutDuration) {
                                    val remaining = (lockoutDuration - timeElapsed) / 60000 + 1
                                    return Result.failure(Exception("Account blocked. Try again in $remaining minute(s)."))
                                } else {
                                    // Lockout expired, treat as fresh start
                                    failedAttempts = 0
                                }
                            }

                            val passwordHash = rs.getString("password_hash")
                            if (passwordHash.isNullOrBlank()) {
                                return Result.failure(Exception("This user does not have a password yet"))
                            }

                            if (!PasswordHash.verifyPassword(plainPassword, passwordHash)) {
                                val newFailedAttempts = failedAttempts + 1
                                if (newFailedAttempts >= 3) {
                                    val now = Timestamp(currentTime)
                                    updateLoginStatus(conn, personId, newFailedAttempts, now)
                                    return Result.failure(Exception("Wrong password. Account blocked for 1 minute."))
                                } else {
                                    updateLoginStatus(conn, personId, newFailedAttempts, null)
                                    return Result.failure(Exception("Wrong password. Attempt $newFailedAttempts of 3."))
                                }
                            }

                            // Success: Reset failed attempts and block timestamp
                            updateLoginStatus(conn, personId, 0, null)

                            val user = User(
                                id = personId,
                                name = rs.getString("name"),
                                email = rs.getString("email"),
                                role = rs.getString("role"),
                                birthDate = rs.getString("birth_date"),
                                address = rs.getString("address")
                            )
                            Result.success(user)
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun updateLoginStatus(conn: Connection, personId: Int, failedAttempts: Int, blockedAt: Timestamp?) {
            val sql = "UPDATE person SET failed_attempts = ?, blocked_at = ? WHERE person_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, failedAttempts)
                stmt.setTimestamp(2, blockedAt)
                stmt.setInt(3, personId)
                stmt.executeUpdate()
            }
        }

        fun resetPassword(email: String, newPlainPassword: String): Result<Unit> {
            val hash = PasswordHash.hashPassword(newPlainPassword)
            val connection = getConnection() ?: return Result.failure(Exception("No connection"))
            return try {
                connection.use { conn ->
                    val sql = "UPDATE person SET password_hash = ?, failed_attempts = 0, blocked_at = NULL WHERE LOWER(email) = LOWER(?)"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, hash)
                        stmt.setString(2, email)
                        if (stmt.executeUpdate() > 0) Result.success(Unit) else Result.failure(Exception("Email not found"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun confirmUser(personId: Int): Result<Unit> {
            val connection = getConnection() ?: return Result.failure(Exception("No connection"))
            return try {
                connection.use { conn ->
                    val sql = "UPDATE person SET is_confirmed = 1, confirmed_at = CURRENT_TIMESTAMP WHERE person_id = ?"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setInt(1, personId)
                        if (stmt.executeUpdate() > 0) Result.success(Unit) else Result.failure(Exception("User not found"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun getRecordsForDoctor(doctorId: Int): List<MedicalRecord> {
            return getRecords("WHERE d.doctor_id = ?", doctorId)
        }

        fun getRecordsForPatient(personId: Int): List<MedicalRecord> {
            return getRecords("WHERE pa.person_id = ?", personId)
        }

        fun getAllRecords(): List<MedicalRecord> {
            return getRecords("", null)
        }

        private fun getRecords(whereClause: String, id: Int?): List<MedicalRecord> {
            val list = mutableListOf<MedicalRecord>()
            getConnection()?.use { conn ->
                val sql = """
                    SELECT mr.record_id, p_pat.name as pat_name, p_doc.name as doc_name, mr.diagnosis, mr.treatment_date
                    FROM medical_record mr
                    JOIN patient pa ON mr.patient_id = pa.patient_id
                    JOIN person p_pat ON pa.person_id = p_pat.person_id
                    JOIN doctor d ON mr.doctor_id = d.doctor_id
                    JOIN person p_doc ON d.doctor_id = p_doc.person_id
                    $whereClause
                    ORDER BY mr.treatment_date DESC
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    id?.let { stmt.setInt(1, it) }
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(MedicalRecord(
                                rs.getInt("record_id"),
                                rs.getString("pat_name"),
                                rs.getString("doc_name"),
                                rs.getString("diagnosis"),
                                rs.getString("treatment_date")
                            ))
                        }
                    }
                }
            }
            return list
        }
    }
}
