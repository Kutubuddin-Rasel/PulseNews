package com.example.newsapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ─── PRIMARY · Ink Cobalt ─────────────────────────────────
val PrimaryLight        = Color(0xFF2E4BDE)
val OnPrimaryLight      = Color(0xFFFFFFFF)
val PrimaryContainerLight= Color(0xFFDCE2FF)
val OnPrimaryContainerLight = Color(0xFF001154)

val PrimaryDark         = Color(0xFFB6C2FF)
val OnPrimaryDark       = Color(0xFF102369)
val PrimaryContainerDark= Color(0xFF2C3FB8)
val OnPrimaryContainerDark = Color(0xFFDDE2FF)

// ─── SECONDARY · Terracotta ───────────────────────────────
val SecondaryLight       = Color(0xFFB25738)
val OnSecondaryLight     = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFFFDBCD)
val OnSecondaryContainerLight = Color(0xFF3A1408)

val SecondaryDark        = Color(0xFFFFB59C)
val OnSecondaryDark      = Color(0xFF5B1C09)
val SecondaryContainerDark = Color(0xFF6B2D17)
val OnSecondaryContainerDark = Color(0xFFFFDBCD)

// ─── TERTIARY · Editorial Sage (subtle accent) ────────────
val TertiaryLight        = Color(0xFF4D6A57)
val TertiaryContainerLight = Color(0xFFD0E8D8)
val TertiaryDark         = Color(0xFFB4D2BD)
val TertiaryContainerDark = Color(0xFF334B3D)

// ─── SURFACE · Warm paper (light) / Refined ink (dark) ────
val BackgroundLight      = Color(0xFFFAF8F4)
val SurfaceLight         = Color(0xFFFAF8F4)
val SurfaceDimLight      = Color(0xFFDAD7CE)
val SurfaceBrightLight   = Color(0xFFFAF8F4)
val SurfaceContainerLowestLight  = Color(0xFFFFFFFF)
val SurfaceContainerLowLight     = Color(0xFFF4F1EB)
val SurfaceContainerLight        = Color(0xFFEDEAE2)
val SurfaceContainerHighLight    = Color(0xFFE7E3D9)
val SurfaceContainerHighestLight = Color(0xFFE0DCD0)

val BackgroundDark       = Color(0xFF0E0F13)
val SurfaceDark          = Color(0xFF0E0F13)
val SurfaceDimDark       = Color(0xFF0A0B0E)
val SurfaceBrightDark    = Color(0xFF34373F)
val SurfaceContainerLowestDark  = Color(0xFF08090C)
val SurfaceContainerLowDark     = Color(0xFF14161B)
val SurfaceContainerDark        = Color(0xFF1A1D23)
val SurfaceContainerHighDark    = Color(0xFF22252C)
val SurfaceContainerHighestDark = Color(0xFF2A2E36)

// ─── ON-SURFACE / OUTLINE ─────────────────────────────────
val OnBackgroundLight    = Color(0xFF14140E)
val OnSurfaceLight       = Color(0xFF14140E)
val OnSurfaceVariantLight= Color(0xFF5A5749)
val OutlineLight         = Color(0xFFC9C4B6)
val OutlineVariantLight  = Color(0xFFDDD8CB)

val OnBackgroundDark     = Color(0xFFECE9DF)
val OnSurfaceDark        = Color(0xFFECE9DF)
val OnSurfaceVariantDark = Color(0xFFB2AE9F)
val OutlineDark          = Color(0xFF3A3E47)
val OutlineVariantDark   = Color(0xFF2A2E36)

// ─── SEMANTIC ─────────────────────────────────────────────
val SuccessLight = Color(0xFF1E8E5A);  val SuccessDark = Color(0xFF85D6A8)
val WarningLight = Color(0xFFC77700);  val WarningDark = Color(0xFFFFB870)
val ErrorLight   = Color(0xFFC62A38);  val ErrorDark   = Color(0xFFFFB4AB)
val OnErrorLight = Color(0xFFFFFFFF);  val OnErrorDark = Color(0xFF690005)

// ─── ACCENT · Aurora gradient ─────────────────────────────
// The signature expressive moment. Use SPARINGLY — one per
// screen, max — on featured stories, AI briefings, breaking
// news ribbons, the For-You header. Never on plain buttons.
val AccentCoral   = Color(0xFFFF7A59)
val AccentMagenta = Color(0xFFE94A8A)
val AccentViolet  = Color(0xFF7B5BFF)
val OnAccent      = Color(0xFFFFFFFF)

val AccentGradient: Brush = Brush.linearGradient(
    colors = listOf(AccentCoral, AccentMagenta, AccentViolet)
)