package com.example.newsapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.example.newsapp.ui.tokens.NewsRadius

val NewsShapes = Shapes(
    extraSmall = RoundedCornerShape(NewsRadius.xs),
    small = RoundedCornerShape(NewsRadius.sm),
    medium = RoundedCornerShape(NewsRadius.md),
    large = RoundedCornerShape(NewsRadius.card),
    extraLarge = RoundedCornerShape(NewsRadius.pill)
)
