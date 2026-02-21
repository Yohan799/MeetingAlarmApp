package com.alarmify.meetings.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.alarmify.meetings.R
import com.alarmify.meetings.data.auth.FathomAuthManager
import com.alarmify.meetings.data.repository.AccountRepository
import com.alarmify.meetings.databinding.FragmentSettingsBinding
import com.alarmify.meetings.databinding.ItemAccountBinding
import com.alarmify.meetings.ui.auth.SignInActivity

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var accountRepository: AccountRepository
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var fathomAuthManager: FathomAuthManager
    
    private val connectedAccounts = mutableListOf<String>()
    private lateinit var accountsAdapter: AccountsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        setupProfile()
        setupAccountsList()
        setupFathomIntegration()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh state when returning to this tab
        refreshAccountsList()
        updateFathomState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeComponents() {
        val context = requireContext()
        accountRepository = AccountRepository(context)
        fathomAuthManager = FathomAuthManager(context)
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    private fun setupProfile() {
        // Use the primary signed-in account for the profile header
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            binding.tvProfileName.text = account.displayName ?: "User"
            binding.tvProfileEmail.text = account.email ?: ""
            
            if (account.photoUrl != null) {
                Glide.with(this)
                    .load(account.photoUrl)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }
    }

    private fun setupAccountsList() {
        accountsAdapter = AccountsAdapter(connectedAccounts) { email ->
             removeAccount(email)
        }
        
        binding.rvAccounts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountsAdapter
        }
        
        refreshAccountsList()
    }
    
    private fun refreshAccountsList() {
        connectedAccounts.clear()
        connectedAccounts.addAll(accountRepository.getAuthorizedAccounts())
        accountsAdapter.notifyDataSetChanged()
    }

    private fun setupFathomIntegration() {
       updateFathomState()
    }
    
    private fun updateFathomState() {
        // Handle in onResume mostly, but initial setup here
        if (fathomAuthManager.isAuthorized()) {
            binding.btnFathomAction.text = getString(R.string.disconnect)
            binding.btnFathomAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_core))
            binding.ivFathom.setColorFilter(ContextCompat.getColor(requireContext(), R.color.teal_core))
        } else {
            binding.btnFathomAction.text = getString(R.string.connect)
            binding.btnFathomAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_core))
            binding.ivFathom.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun setupClickListeners() {
        binding.btnAddAccount.setOnClickListener {
             // Re-launch SignInActivity to add new account
             val intent = Intent(requireContext(), SignInActivity::class.java)
             startActivity(intent)
        }
        
        binding.btnFathomAction.setOnClickListener {
            if (fathomAuthManager.isAuthorized()) {
                // Disconnect
                AlertDialog.Builder(requireContext())
                    .setTitle("Disconnect Fathom?")
                    .setMessage("You will stop receiving meeting summaries.")
                    .setPositiveButton("Disconnect") { _, _ ->
                        fathomAuthManager.signOut()
                        updateFathomState()
                        Toast.makeText(requireContext(), "Disconnected Fathom", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Connect
                fathomAuthManager.startAuth(requireContext())
                // Auth flow happens in browser -> redirects to MainActivity -> handled there
                // MainActivity should probably notify us or refresh, but since we are in a fragment,
                // onResume might catch it if MainActivity re-attaches or if we just returned.
                // NOTE: MainActivity handles the deep link.
            }
        }
        
        binding.btnSignOutAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sign Out All")
                .setMessage("Are you sure you want to remove all accounts and sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    googleSignInClient.signOut().addOnCompleteListener {
                        accountRepository.clearAll()
                        fathomAuthManager.signOut()
                        
                        startActivity(Intent(requireContext(), SignInActivity::class.java))
                        activity?.finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun removeAccount(email: String) {
        // Prevent removing the LAST account (or current active one if we want strictness)
        // For now, simplify: if 1 account left, warn them to use Sign Out All
        if (connectedAccounts.size <= 1) {
            Toast.makeText(requireContext(), "Use 'Sign Out All' to remove the last account.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Remove Account")
            .setMessage("Remove $email from this app?")
            .setPositiveButton("Remove") { _, _ ->
                accountRepository.removeAccount(email)
                refreshAccountsList()
                Toast.makeText(requireContext(), "Account removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // Internal Adapter for Accounts List
    inner class AccountsAdapter(
        private val accounts: List<String>,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<AccountsAdapter.AccountViewHolder>() {

        inner class AccountViewHolder(private val binding: ItemAccountBinding) :
            RecyclerView.ViewHolder(binding.root) {
            
            fun bind(email: String) {
                binding.tvAccountEmail.text = email
                binding.btnRemoveAccount.setOnClickListener {
                    onDeleteClick(email)
                }
                
                // Hide remove button for the "current" primary account?
                // SignInActivity logic: last signed in is primary.
                // For simplicity, allow removing any.
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
             // We need item_account.xml. It might not exist yet!
             // Let's assume we need to create it or reuse something.
             // I'll create a simple bind here if binding class exists, 
             // but 'ItemAccountBinding' implies 'item_account.xml'.
             return AccountViewHolder(
                 ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
             )
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            holder.bind(accounts[position])
        }

        override fun getItemCount() = accounts.size
    }
}
