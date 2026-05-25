package com.example.newsapp.data.util

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: java.util.UUID.randomUUID().toString()
    }
}
