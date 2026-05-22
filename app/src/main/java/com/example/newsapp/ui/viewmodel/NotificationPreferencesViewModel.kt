package com.example.newsapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.repository.NotificationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val repository: NotificationPreferencesRepository
) : ViewModel() {

    val subscribedTopics: StateFlow<Set<String>> = repository.subscribedTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val quietHoursEnabled: StateFlow<Boolean> = repository.quietHoursEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quietHoursStartMinutes: StateFlow<Int> = repository.quietHoursStartMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 22 * 60)

    val quietHoursEndMinutes: StateFlow<Int> = repository.quietHoursEndMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7 * 60)

    val maxDailyNotifications: StateFlow<Int> = repository.maxDailyNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    fun toggleTopic(topic: String, subscribe: Boolean) {
        viewModelScope.launch {
            val currentTopics = subscribedTopics.value.toMutableSet()
            if (subscribe) {
                currentTopics.add(topic)
            } else {
                currentTopics.remove(topic)
            }
            repository.setTopics(currentTopics)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setQuietHoursEnabled(enabled)
        }
    }

    fun setQuietHours(startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            repository.setQuietHours(startMinutes, endMinutes)
        }
    }

    fun setMaxDailyNotifications(max: Int) {
        viewModelScope.launch {
            repository.setMaxDailyNotifications(max)
        }
    }
}
