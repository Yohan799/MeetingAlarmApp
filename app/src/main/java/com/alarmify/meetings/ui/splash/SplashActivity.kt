package com.alarmify.meetings.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.alarmify.meetings.databinding.ActivitySplashBinding
import com.alarmify.meetings.ui.auth.SignInActivity
import com.alarmify.meetings.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DELAY = 2200L // Increased slightly for animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateUI()

        // Check if user is already signed in
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAuthentication()
        }, SPLASH_DELAY)
    }

    private fun animateUI() {
        val logo = binding.ivLogo
        val ringInner = binding.ringInner
        val ringOuter = binding.ringOuter
        val title = binding.tvAppName
        val tagline = binding.tvTagline

        // Initial states
        logo.alpha = 0f
        logo.scaleX = 0.8f
        logo.scaleY = 0.8f
        
        title.alpha = 0f
        title.translationY = 50f
        
        tagline.alpha = 0f
        tagline.translationY = 30f

        // Rings setup
        ringInner.scaleX = 0f
        ringInner.scaleY = 0f
        ringInner.alpha = 0f
        
        ringOuter.scaleX = 0f
        ringOuter.scaleY = 0f
        ringOuter.alpha = 0f

        // Animators
        val logoFade = ObjectAnimator.ofFloat(logo, View.ALPHA, 1f).setDuration(800)
        val logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f).setDuration(800)
        val logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f).setDuration(800)

        val titleFade = ObjectAnimator.ofFloat(title, View.ALPHA, 1f).setDuration(600)
        val titleMove = ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, 0f).setDuration(600)
        titleFade.startDelay = 300
        titleMove.startDelay = 300

        val taglineFade = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0.65f).setDuration(600)
        val taglineMove = ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, 0f).setDuration(600)
        taglineFade.startDelay = 500
        taglineMove.startDelay = 500

        // Ring Expansion
        val ringInnerScaleX = ObjectAnimator.ofFloat(ringInner, View.SCALE_X, 1f).setDuration(1200)
        val ringInnerScaleY = ObjectAnimator.ofFloat(ringInner, View.SCALE_Y, 1f).setDuration(1200)
        val ringInnerFade = ObjectAnimator.ofFloat(ringInner, View.ALPHA, 0.2f).setDuration(1200)

        val ringOuterScaleX = ObjectAnimator.ofFloat(ringOuter, View.SCALE_X, 1f).setDuration(1400)
        val ringOuterScaleY = ObjectAnimator.ofFloat(ringOuter, View.SCALE_Y, 1f).setDuration(1400)
        val ringOuterFade = ObjectAnimator.ofFloat(ringOuter, View.ALPHA, 0.1f).setDuration(1400)

        val set = AnimatorSet()
        set.playTogether(
            logoFade, logoScaleX, logoScaleY,
            titleFade, titleMove,
            taglineFade, taglineMove,
            ringInnerScaleX, ringInnerScaleY, ringInnerFade,
            ringOuterScaleX, ringOuterScaleY, ringOuterFade
        )
        set.interpolator = DecelerateInterpolator()
        set.start()
    }

    private fun checkUserAuthentication() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        
        val intent = if (account != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, SignInActivity::class.java)
        }
        
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
