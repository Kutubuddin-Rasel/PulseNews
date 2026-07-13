package com.example.newsapp.data.util

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Returns the first input that is a valid ISO 3166-1 alpha-2 code (uppercased),
 * else null. Pure so the SIM→network→locale fallback order is unit-testable
 * without an Android runtime.
 */
fun pickRegion(network: String?, sim: String?, locale: String?): String? =
    sequenceOf(network, sim, locale)
        .map { it?.trim()?.uppercase(Locale.ROOT).orEmpty() }
        .firstOrNull { it.matches(Regex("^[A-Z]{2}$")) }

/**
 * Permission-free best-effort home region. Prefers the live network country,
 * then the SIM country, then the device locale's country — no location
 * permission, no GPS, just a soft nudge for the geo ranker.
 */
class RegionDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun detectRegion(): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return pickRegion(
            network = tm?.networkCountryIso,
            sim = tm?.simCountryIso,
            locale = Locale.getDefault().country,
        )
    }
}
