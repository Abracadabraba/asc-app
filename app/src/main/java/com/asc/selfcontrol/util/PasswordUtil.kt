package com.asc.selfcontrol.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Password is NEVER stored in plaintext. Only a salted hash is kept.
 * This means even the app itself (and its creator) cannot retrieve the
 * original password once it has been set.
 */
object PasswordUtil {

    private const val ITERATIONS = 20_000

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(password: String, saltB64: String): String {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        var data = (password.toByteArray(Charsets.UTF_8) + salt)
        val digest = MessageDigest.getInstance("SHA-256")
        repeat(ITERATIONS) {
            data = digest.digest(data)
        }
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    fun verify(password: String, saltB64: String, expectedHash: String): Boolean {
        return hash(password, saltB64) == expectedHash
    }
}
