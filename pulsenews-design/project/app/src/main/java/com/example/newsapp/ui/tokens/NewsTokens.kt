package com.example.newsapp.ui.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object NewsSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object NewsRadius {
    val card = 20.dp
    val chip = 14.dp
}

object NewsElevation {
    val card = 4.dp
}

object NewsMotion {
    const val fast = 150
    const val normal = 220
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
