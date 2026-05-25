package com.example.newsapp.Hilt

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class FirebaseTokenInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val user = FirebaseAuth.getInstance().currentUser
        
        if (user != null) {
            try {
                // Use getIdToken(false) to use the cached token if valid, 
                // avoiding unnecessary network calls and latency.
                val task = user.getIdToken(false)
                // Tasks.await blocks the current thread until the task completes.
                // This is safe because OkHttp interceptors run on background network threads.
                val tokenResult = Tasks.await(task)
                val token = tokenResult.token
                
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            } catch (e: Exception) {
                // If token fetching fails (e.g., offline or expired and refresh fails),
                // proceed without the token. The backend will return a 401 Unauthorized if required.
            }
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
