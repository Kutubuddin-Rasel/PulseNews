package com.example.newsapp.domain.usecase.auth

import android.content.Context
import com.example.newsapp.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(context: Context): Result<com.google.firebase.auth.FirebaseUser> {
        return authRepository.signInWithGoogle(context)
    }
}
