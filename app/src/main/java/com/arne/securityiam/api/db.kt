package com.arne.securityiam.api

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
                println("=== DB CONNECTION SUCCESS: $conn ===")
                conn
            } catch (e: Exception) {
                println("=== DB CONNECTION FAILED: ${e::class.java.simpleName}: ${e.message} ===")
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
                                role = rs.getString("role")
                            )

                            Result.success(user)
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun seedExistingPersonLogins(): Result<Int> {
            val usersToSeed = listOf(
                Triple(1, "doctor.thomas@iam.test", "doctor1234"),
                Triple(2, "doctor.wim@iam.test", "doctor1234"),
                Triple(3, "robbie.uijttenboogaard@iam.test", "patient1234"),
                Triple(4, "frans.leijdekkers@iam.test", "patient1234"),
                Triple(5, "zuster.joke@iam.test", "nurse1234"),
                Triple(6, "zuster.anna@iam.test", "doctor1234"),
                Triple(7, "emma.devries@iam.test", "doctor1234"),
                Triple(8, "lars.boer@iam.test", "patient1234"),
                Triple(9, "sophie.klein@iam.test", "patient1234"),
                Triple(10, "bram.vandijk@iam.test", "patient1234"),
                Triple(11, "eva.jansen@iam.test", "nurse1234")
            )

            val connection = getConnection()
                ?: return Result.failure(Exception("Could not connect to the database"))

            return try {
                connection.use { conn ->
                    val sql = """
                        UPDATE person
                        SET email = ?, password_hash = ?
                        WHERE person_id = ?
                          AND (email IS NULL OR password_hash IS NULL)
                    """.trimIndent()

                    conn.prepareStatement(sql).use { stmt ->
                        var changedRows = 0

                        for ((personId, email, password) in usersToSeed) {
                            val passwordHash = PasswordHash.hashPassword(password)

                            stmt.setString(1, email)
                            stmt.setString(2, passwordHash)
                            stmt.setInt(3, personId)
                            changedRows += stmt.executeUpdate()
                        }

                        Result.success(changedRows)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
