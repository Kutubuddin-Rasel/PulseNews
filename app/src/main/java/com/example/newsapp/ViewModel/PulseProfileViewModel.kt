package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.util.EngagementTracker
import com.example.newsapp.domain.model.GamificationProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.example.newsapp.data.util.AuthManager
import kotlinx.coroutines.launch

@HiltViewModel
class PulseProfileViewModel @Inject constructor(
    engagementTracker: EngagementTracker,
    private val authManager: AuthManager
) : ViewModel() {

    val profile: StateFlow<GamificationProfile> = engagementTracker.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GamificationProfile()
    )
    
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
