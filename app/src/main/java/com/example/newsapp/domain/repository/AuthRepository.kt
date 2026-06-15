package com.example.newsapp.domain.repository

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser>
    fun signOut()
}
