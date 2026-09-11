package com.tdminsight.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Simple local-only account store backed by SharedPreferences + JSON.
 *
 * No backend/cloud dependency (consistent with the project scope): accounts are
 * created and authenticated entirely on-device. Passwords are never stored in
 * plain text — each is hashed with a random per-user salt using SHA-256.
 */
object AuthStore {

    private const val PREFS_NAME = "tdm_insight_auth"
    private const val KEY_ACCOUNTS = "accounts_json"
    private const val KEY_CURRENT_USER = "current_user"

    data class Account(val username: String, val salt: String, val hash: String)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalize(username: String) = username.trim().lowercase()

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadAccounts(context: Context): MutableList<Account> {
        val raw = prefs(context).getString(KEY_ACCOUNTS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                Account(o.getString("username"), o.getString("salt"), o.getString("hash"))
            }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun persistAccounts(context: Context, accounts: List<Account>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("username", a.username)
                put("salt", a.salt)
                put("hash", a.hash)
            })
        }
        prefs(context).edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }

    /** Returns an error message, or null on success. */
    fun register(context: Context, username: String, password: String): String? {
        val name = normalize(username)
        if (name.length < 3) return "Username must be at least 3 characters."
        if (password.length < 4) return "Password must be at least 4 characters."

        val accounts = loadAccounts(context)
        if (accounts.any { it.username == name }) return "That username is already taken."

        val salt = randomSalt()
        accounts.add(Account(name, salt, hash(password, salt)))
        persistAccounts(context, accounts)

        prefs(context).edit().putString(KEY_CURRENT_USER, name).apply()
        return null
    }

    /** Returns an error message, or null on success. */
    fun login(context: Context, username: String, password: String): String? {
        val name = normalize(username)
        val account = loadAccounts(context).find { it.username == name }
            ?: return "No account found with that username."
        if (hash(password, account.salt) != account.hash) return "Incorrect password."

        prefs(context).edit().putString(KEY_CURRENT_USER, name).apply()
        return null
    }

    fun logout(context: Context) {
        prefs(context).edit().remove(KEY_CURRENT_USER).apply()
    }

    fun currentUser(context: Context): String? = prefs(context).getString(KEY_CURRENT_USER, null)

    fun isLoggedIn(context: Context): Boolean = currentUser(context) != null
}
