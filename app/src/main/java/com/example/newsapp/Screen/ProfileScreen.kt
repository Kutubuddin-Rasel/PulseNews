package com.example.newsapp.Screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {
    val currentUser = authManager.currentUser

    fun signIn() {
        viewModelScope.launch {
            authManager.signInWithGoogle()
        }
    }

    fun signOut() {
        authManager.signOut()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Sync") },
                navigationIcon = {
                    // Back button here (omitted for brevity, assume standard implementation)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (user != null) {
                Text(
                    text = "Signed in as",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = user?.displayName ?: user?.email ?: "Unknown User",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your saved articles and preferences are being securely synced to your cloud profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = { viewModel.signOut() }) {
                    Text("Sign Out")
                }
            } else {
                Text(
                    text = "Not Signed In",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Sign in to enable cross-device sync for your saved articles and algorithm preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = { viewModel.signIn() }) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}
