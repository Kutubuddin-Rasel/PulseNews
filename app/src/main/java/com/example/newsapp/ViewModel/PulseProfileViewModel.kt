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

import com.example.newsapp.domain.model.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class PulseProfileViewModel @Inject constructor(
    engagementTracker: EngagementTracker,
    private val authManager: AuthManager
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    val profile: StateFlow<GamificationProfile> = engagementTracker.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GamificationProfile()
    )
    
    val currentUser = authManager.currentUser

    fun signIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activityContext)
            if (result.isFailure) {
                _events.emit(UiEvent.Generic("Sign in failed. Ensure you have a Google Account on this device."))
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }
}
