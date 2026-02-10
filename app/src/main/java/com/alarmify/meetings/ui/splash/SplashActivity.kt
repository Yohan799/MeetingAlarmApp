package com.alarmify.meetings.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.alarmify.meetings.databinding.ActivitySplashBinding
import com.alarmify.meetings.ui.auth.SignInActivity
import com.alarmify.meetings.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already signed in
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAuthentication()
        }, SPLASH_DELAY)
    }

    private fun checkUserAuthentication() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        
        if (account != null) {
            // User is signed in, go to main activity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not signed in, go to sign in activity
            startActivity(Intent(this, SignInActivity::class.java))
        }
        finish()
    }
}

