package com.arne.securityiam.utils

object PasswordValidator {
    fun validate(password: String): Result<Unit> {
        if (password.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters long"))
        }
        if (!password.any { it.isLetter() }) {
            return Result.failure(Exception("Password must contain at least 1 letter"))
        }
        if (!password.any { it.isDigit() }) {
            return Result.failure(Exception("Password must contain at least 1 number"))
        }
        val specialCharacters = "!@#\$%^&*()-_=+[]{}|;:',.<>?/`~"
        if (!password.any { specialCharacters.contains(it) }) {
            return Result.failure(Exception("Password must contain at least 1 special character"))
        }
        return Result.success(Unit)
    }
}
