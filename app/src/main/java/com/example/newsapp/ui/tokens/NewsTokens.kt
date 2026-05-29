package com.example.newsapp.ui.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object NewsSpacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 24.dp
    val xxl = 32.dp
}

object NewsRadius {
    val xs   = 6.dp
    val sm   = 10.dp
    val md   = 14.dp     // inner image, search input
    val card = 20.dp     // article cards, ai card
    val lg   = 24.dp     // stage, sheet
    val pill = 999.dp    // chips, cta
}

object NewsElevation {
    val none    = 0.dp
    val card    = 0.dp    // flat — depth comes from surfaceContainer tiers
    val raised  = 2.dp
    val sheet   = 6.dp
}

object NewsStroke {
    val thin    = 1.dp
    val regular = 1.5.dp
}

object NewsMotion {
    const val fast    = 120
    const val normal  = 220
    const val slow    = 400
    val standardEasing  = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedEase  = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val pressScale = 0.98f
}

object NewsImage {
    val heroAspect    = 16f / 9f
    val featuredAspect= 1.85f
    val heroHeight    = 172.dp
    val featuredHeight= 220.dp
}