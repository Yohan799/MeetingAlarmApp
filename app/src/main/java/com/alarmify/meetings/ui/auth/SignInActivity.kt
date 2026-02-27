package com.alarmify.meetings.ui.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.alarmify.meetings.R
import com.alarmify.meetings.data.repository.AccountRepository
import com.alarmify.meetings.databinding.ActivitySignInBinding
import com.alarmify.meetings.ui.main.MainActivity

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var accountRepository: AccountRepository
    
    companion object {
        private const val TAG = "SignInActivity"
    }
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Log.e(TAG, "Sign in failed: ${e.statusCode}", e)
            Toast.makeText(this, "Sign in failed: ${e.statusCode} - ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        accountRepository = AccountRepository(this)
        
        animateEntrance()
        setupGoogleSignIn()
        updateAccountList()
        setupClickListeners()
    }
    
    private fun animateEntrance() {
        val card = binding.cardSignIn
        val orbs = listOf(binding.brandGlowOuter, binding.brandGlowInner)
        
        // Initial state
        card.alpha = 0f
        card.scaleX = 0.95f
        card.scaleY = 0.95f
        card.translationY = 50f
        
        orbs.forEach { it.alpha = 0f }

        // Card Animation
        val cardFade = ObjectAnimator.ofFloat(card, View.ALPHA, 1f).setDuration(600)
        val cardScaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, 1f).setDuration(800)
        val cardScaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, 1f).setDuration(800)
        val cardMove = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, 0f).setDuration(800)
        
        cardScaleX.interpolator = OvershootInterpolator(1.2f)
        cardScaleY.interpolator = OvershootInterpolator(1.2f)
        
        // Orbs Animation
        val orbAnims = orbs.map { 
            ObjectAnimator.ofFloat(it, View.ALPHA, if(it == binding.brandGlowInner) 0.3f else 0.15f).setDuration(1200)
        }

        val set = AnimatorSet()
        set.playTogether(cardFade, cardScaleX, cardScaleY, cardMove)
        set.playTogether(orbAnims)
        set.startDelay = 200
        set.start()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    private fun updateAccountList() {
        val accounts = accountRepository.getAuthorizedAccounts()
        binding.accountsListContainer.removeAllViews()
        
        // Add view for each account
        accounts.forEach { email ->
            val accountView = layoutInflater.inflate(R.layout.item_account, binding.accountsListContainer, false)
            val tvEmail = accountView.findViewById<TextView>(R.id.tvAccountEmail)
            val btnRemove = accountView.findViewById<View>(R.id.btnRemoveAccount)
            
            tvEmail.text = email
            btnRemove.setOnClickListener {
                removeAccount(email)
            }
            binding.accountsListContainer.addView(accountView)
        }
        
        // Toggle Continue button
        if (accounts.isNotEmpty()) {
            binding.btnContinue.visibility = View.VISIBLE
        } else {
            binding.btnContinue.visibility = View.GONE
        }
        
        // Toggle Add Button based on limit
        if (accountRepository.isLimitReached()) {
            binding.btnAddAccount.visibility = View.GONE
            binding.tvLimitWarning.visibility = View.VISIBLE
        } else {
            binding.btnAddAccount.visibility = View.VISIBLE
            binding.tvLimitWarning.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnAddAccount.setOnClickListener {
            // Sign out any current session to allow choosing a new account
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                signInLauncher.launch(signInIntent)
            }
        }
        
        binding.btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
             val email = account.email
             if (email != null) {
                 if (accountRepository.addAccount(email)) {
                     Toast.makeText(this, "Account added: $email", Toast.LENGTH_SHORT).show()
                     updateAccountList()
                 } else {
                     Toast.makeText(this, "Limit reached or error", Toast.LENGTH_SHORT).show()
                 }
             }
        } else {
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeAccount(email: String) {
        accountRepository.removeAccount(email)
        updateAccountList()
        Toast.makeText(this, "Removed $email", Toast.LENGTH_SHORT).show()
    }
}
