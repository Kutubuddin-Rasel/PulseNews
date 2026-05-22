package com.example.newsapp.data.util

import com.example.newsapp.domain.util.ClockProvider
import javax.inject.Inject

class SystemClockProvider @Inject constructor() : ClockProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
