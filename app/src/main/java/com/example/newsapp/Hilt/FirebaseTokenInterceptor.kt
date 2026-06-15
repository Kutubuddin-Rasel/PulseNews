package com.example.newsapp.Hilt

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches a Firebase ID token ONLY to requests that opt in via the marker header
 * `X-Pulse-Auth: required` (declared per-endpoint in [com.example.newsapp.Api.PulseBackendApi]).
 *
 * - Audit A4 (OCP): auth policy lives with the endpoint contract, not as path-matching here.
 *   Public, edge-cacheable GETs (feed/search/trending/taxonomy/meta) stay anonymous so a
 *   per-user `Authorization` header never defeats Cloudflare/OkHttp shared caching.
 * - Audit A1: the token is read from Firebase's local cache via `getIdToken(false)`, which
 *   refreshes over the network only when the token is actually expired — instead of forcing a
 *   Google token round-trip before every single request.
 */
class FirebaseTokenInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Only authed endpoints opt in; everything else proceeds token-free (cacheable).
        if (original.header(AUTH_MARKER_HEADER) != AUTH_MARKER_VALUE) {
            return chain.proceed(original)
        }

        // Strip the internal marker so it never leaves the device.
        val requestBuilder = original.newBuilder().removeHeader(AUTH_MARKER_HEADER)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                // getIdToken(false): cached token; SDK refreshes only when expired.
                // Tasks.await blocks this OkHttp network thread (safe — never the main thread).
                val token = Tasks.await(user.getIdToken(false)).token
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            } catch (e: Exception) {
                // Offline / refresh failure: proceed without a token; backend returns 401 if required.
            }
        }

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        /** Marker header the [PulseBackendApi][com.example.newsapp.Api.PulseBackendApi] sets on authed calls. */
        const val AUTH_MARKER_HEADER = "X-Pulse-Auth"
        const val AUTH_MARKER_VALUE = "required"
    }
}
