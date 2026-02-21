package com.alarmify.meetings.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository for managing authorized Google accounts.
 * Stores the list of email addresses that have been authorized.
 */
class AccountRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "meeting_alarm_accounts"
        private const val KEY_ACCOUNTS = "authorized_accounts"
        private const val MAX_ACCOUNTS = 5
    }

    /**
     * Get the list of all authorized account emails.
     */
    fun getAuthorizedAccounts(): Set<String> {
        val json = sharedPreferences.getString(KEY_ACCOUNTS, null)
        return if (json != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptySet()
        }
    }

    /**
     * Add a new account email.
     * @return true if added successfully, false if limit reached or already exists.
     */
    fun addAccount(email: String): Boolean {
        val currentAccounts = getAuthorizedAccounts().toMutableSet()
        
        if (currentAccounts.contains(email)) {
            return true // Already exists, consider it a success
        }

        if (currentAccounts.size >= MAX_ACCOUNTS) {
            return false // Limit reached
        }

        currentAccounts.add(email)
        saveAccounts(currentAccounts)
        return true
    }

    /**
     * Remove an account email.
     */
    fun removeAccount(email: String) {
        val currentAccounts = getAuthorizedAccounts().toMutableSet()
        if (currentAccounts.remove(email)) {
            saveAccounts(currentAccounts)
        }
    }

    /**
     * Check if the account limit has been reached.
     */
    fun isLimitReached(): Boolean {
        return getAuthorizedAccounts().size >= MAX_ACCOUNTS
    }

    private fun saveAccounts(accounts: Set<String>) {
        val json = gson.toJson(accounts)
        sharedPreferences.edit().putString(KEY_ACCOUNTS, json).apply()
    }
    
    /**
     * Clear all accounts (e.g. on full sign out)
     */
    fun clearAll() {
        sharedPreferences.edit().remove(KEY_ACCOUNTS).apply()
    }
}
