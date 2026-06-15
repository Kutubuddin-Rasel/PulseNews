package com.example.newsapp.domain.usecase.auth

import com.google.firebase.auth.FirebaseUser
import com.example.newsapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<FirebaseUser?> {
        return authRepository.currentUser
    }
}
