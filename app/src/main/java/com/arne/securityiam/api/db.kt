package com.arne.securityiam.api

import com.arne.securityiam.models.MedicalRecord
import com.arne.securityiam.models.User
import com.arne.securityiam.utils.PasswordHash
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

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

        fun registerPatient(name: String, email: String, plainPassword: String): Result<User> {
            val passwordHash = PasswordHash.hashPassword(plainPassword)

            val connection = getConnection()
                ?: return Result.failure(Exception("Could not connect to the database"))

            return try {
                connection.use { conn ->
                    conn.autoCommit = false

                    try {
                        val insertPersonSql = """
                            INSERT INTO person (name, email, password_hash)
                            VALUES (?, ?, ?)
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

                            val passwordHash = rs.getString("password_hash")
                            if (passwordHash.isNullOrBlank()) {
                                return Result.failure(Exception("This user does not have a password yet"))
                            }

                            if (!PasswordHash.verifyPassword(plainPassword, passwordHash)) {
                                return Result.failure(Exception("Wrong password"))
                            }

                            val user = User(
                                id = rs.getInt("person_id"),
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
