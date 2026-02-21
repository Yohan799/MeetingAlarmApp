package com.alarmify.meetings.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class FathomAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "FathomAuthManager"
        private const val AUTH_ENDPOINT = "https://fathom.video/authorize"
        private const val TOKEN_ENDPOINT = "https://fathom.video/external/v1/oauth2/token"
        
        // These should ideally be in BuildConfig, but for now we read from local.properties -> BuildConfig
        // User provided these in the chat
        private const val CLIENT_ID = "fEjgpX0TsmcC-TlY4XAvwvtl30FBw2UtzUXydH2nvk4" 
        private const val CLIENT_SECRET = "tUw_Xc_c2I8tE7XxukEHwv7xX-eAuq96axN5VoXw9rA"
        private const val REDIRECT_URI = "meetingalarm://fathom-callback"
        
        private const val PREFS_NAME = "fathom_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun startAuth(context: Context) {
        val authUri = Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "public_api") // Correct scope from docs
            // .appendQueryParameter("state", "...") // Recommended for security
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, authUri)
    }

    suspend fun handleAuthResponse(intent: Intent): Boolean {
        val data = intent.data ?: return false
        if (!data.toString().startsWith(REDIRECT_URI)) return false

        val code = data.getQueryParameter("code")
        val error = data.getQueryParameter("error")

        if (error != null) {
            Log.e(TAG, "Auth error: $error")
            return false
        }

        if (code != null) {
            return exchangeCodeForToken(code)
        }

        return false
    }

    private suspend fun exchangeCodeForToken(code: String): Boolean = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Token exchange failed: ${response.code} ${response.body?.string()}")
                    return@withContext false
                }

                val responseBody = response.body?.string() ?: return@withContext false
                val json = JSONObject(responseBody)
                
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600)
                
                saveTokens(accessToken, refreshToken, expiresIn)
                Log.d(TAG, "Token exchange successful")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during token exchange", e)
            return@withContext false
        }
    }
    
    // Refresh token implementation could go here, but for MVP we might just re-login or assume long life
    // Fathom tokens expire? Docs said access tokens are short lived.
    
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun isAuthorized(): Boolean {
        return getAccessToken() != null
    }
    
    fun signOut() {
        prefs.edit().clear().apply()
    }

    private fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }
}
