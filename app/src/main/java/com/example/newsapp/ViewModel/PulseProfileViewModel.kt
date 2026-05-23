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

@HiltViewModel
class PulseProfileViewModel @Inject constructor(
    engagementTracker: EngagementTracker
) : ViewModel() {

    val profile: StateFlow<GamificationProfile> = engagementTracker.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GamificationProfile()
    )
}
