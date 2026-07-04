package com.asc.selfcontrol.data

import android.content.Context
import org.json.JSONArray

object Prefs {
    private const val FILE = "asc_prefs"

    private const val KEY_RULES = "rules"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_PASSWORD_SET = "password_set"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_COOLDOWN_UNTIL = "cooldown_until"
    private const val KEY_FORGOT_REQUESTED_AT = "forgot_requested_at"

    const val MAX_ATTEMPTS = 5
    const val COOLDOWN_MILLIS = 24L * 60 * 60 * 1000 // 24 hours
    const val UNLOCK_GRACE_MILLIS = 10L * 60 * 1000   // 10 minutes grace after correct password

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---------- Rules ----------
    fun getRules(ctx: Context): List<Rule> {
        val raw = prefs(ctx).getString(KEY_RULES, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Rule>()
        for (i in 0 until arr.length()) {
            list.add(Rule.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun saveRules(ctx: Context, rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY_RULES, arr.toString()).apply()
    }

    fun addRule(ctx: Context, rule: Rule) {
        val list = getRules(ctx).toMutableList()
        list.add(rule)
        saveRules(ctx, list)
    }

    fun removeRuleAt(ctx: Context, index: Int) {
        val list = getRules(ctx).toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            saveRules(ctx, list)
        }
    }

    // ---------- Password ----------
    fun isPasswordSet(ctx: Context) = prefs(ctx).getBoolean(KEY_PASSWORD_SET, false)

    fun setPassword(ctx: Context, salt: String, hash: String) {
        prefs(ctx).edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash)
            .putBoolean(KEY_PASSWORD_SET, true)
            .apply()
    }

    fun getSalt(ctx: Context) = prefs(ctx).getString(KEY_SALT, "") ?: ""
    fun getHash(ctx: Context) = prefs(ctx).getString(KEY_HASH, "") ?: ""

    /** Clears password so the user can set a brand new one. Only reachable after 24h cooldown. */
    fun resetPassword(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putBoolean(KEY_PASSWORD_SET, false)
            .remove(KEY_FORGOT_REQUESTED_AT)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_COOLDOWN_UNTIL)
            .apply()
    }

    // ---------- Failed attempts / cooldown ----------
    fun getFailedAttempts(ctx: Context) = prefs(ctx).getInt(KEY_FAILED_ATTEMPTS, 0)

    fun registerFailedAttempt(ctx: Context) {
        val attempts = getFailedAttempts(ctx) + 1
        if (attempts >= MAX_ATTEMPTS) {
            prefs(ctx).edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_COOLDOWN_UNTIL, System.currentTimeMillis() + COOLDOWN_MILLIS)
                .apply()
        } else {
            prefs(ctx).edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        }
    }

    fun clearFailedAttempts(ctx: Context) {
        prefs(ctx).edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    fun getCooldownUntil(ctx: Context) = prefs(ctx).getLong(KEY_COOLDOWN_UNTIL, 0L)

    fun isInCooldown(ctx: Context) = System.currentTimeMillis() < getCooldownUntil(ctx)

    // ---------- Forgot password flow (24h wait, then allowed to reset) ----------
    fun requestForgotPassword(ctx: Context) {
        if (prefs(ctx).getLong(KEY_FORGOT_REQUESTED_AT, 0L) == 0L) {
            prefs(ctx).edit().putLong(KEY_FORGOT_REQUESTED_AT, System.currentTimeMillis()).apply()
        }
    }

    fun getForgotRequestedAt(ctx: Context) = prefs(ctx).getLong(KEY_FORGOT_REQUESTED_AT, 0L)

    fun canResetPassword(ctx: Context): Boolean {
        val requestedAt = getForgotRequestedAt(ctx)
        return requestedAt != 0L && System.currentTimeMillis() - requestedAt >= COOLDOWN_MILLIS
    }

    // ---------- Per-app unlock grace period ----------
    private fun graceKey(pkg: String) = "unlock_until_$pkg"

    fun grantGrace(ctx: Context, pkg: String) {
        prefs(ctx).edit()
            .putLong(graceKey(pkg), System.currentTimeMillis() + UNLOCK_GRACE_MILLIS)
            .apply()
    }

    fun isInGrace(ctx: Context, pkg: String): Boolean {
        return System.currentTimeMillis() < prefs(ctx).getLong(graceKey(pkg), 0L)
    }
}
