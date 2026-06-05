package com.example.newsapp.Screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.PulseProfileViewModel
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.ui.components.LocalPulseSnackbar
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.theme.AccentGradient
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseProfileScreen(viewModel: PulseProfileViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val snackbar = LocalPulseSnackbar.current
    val profile by viewModel.profile.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is UiEvent.AlreadySaved, is UiEvent.Saved, is UiEvent.DeleteFailed,
                is UiEvent.NetworkError, is UiEvent.Generic -> event.message
            }
            snackbar.showSnackbar(msg)
        }
    }

    val topCategoriesByShare = remember(profile.categoryReadCounts) {
        val total = profile.categoryReadCounts.values.sum().toFloat().coerceAtLeast(1f)
        profile.categoryReadCounts
            .entries
            .sortedByDescending { it.value }
            .map { (name, count) ->
                val display = name.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
                display to (count / total)
            }
    }

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NewsSpacing.lg)
                    .padding(top = NewsSpacing.lg, bottom = NewsSpacing.xxl),
            ) {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(
                    "YOUR READING, REFLECTED",
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(NewsSpacing.xl))
                AuthCard(
                    user = user,
                    onSignIn = { viewModel.signIn(context) },
                    onSignOut = viewModel::signOut,
                )

                Spacer(Modifier.height(NewsSpacing.lg))
                StreakCard(
                    current = profile.currentStreak,
                    longest = profile.longestStreak
                )

                Spacer(Modifier.height(NewsSpacing.lg))
                ReadingReflectionCard(
                    articlesRead = profile.totalArticlesRead,
                    topCategoriesByShare = topCategoriesByShare,
                )
            }
        }
    }
}

@Composable
private fun AuthCard(
    user: com.google.firebase.auth.FirebaseUser?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(NewsSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (user != null) {
                Text(
                    "SIGNED IN AS",
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(
                    user.displayName ?: user.email ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(NewsSpacing.sm))
                Text(
                    "Saved articles and preferences are synced across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(NewsSpacing.md))
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(NewsRadius.pill),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text("Sign Out", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    "Not Signed In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(NewsSpacing.sm))
                Text(
                    "Sign in to sync your saved articles across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(NewsSpacing.md))
                Button(
                    onClick = onSignIn,
                    shape = RoundedCornerShape(NewsRadius.pill),
                ) {
                    Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StreakCard(current: Int, longest: Int) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(NewsRadius.lg)).background(AccentGradient)
            .padding(NewsSpacing.xl),
    ) {
        Column {
            Text("CURRENT STREAK", style = MetaMono, color = Color.White.copy(alpha = .9f))
            Text("$current", style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp, lineHeight = 84.sp),
                fontWeight = FontWeight.Medium, color = Color.White)
            Text("days reading in a row", style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = .92f))
            Spacer(Modifier.height(NewsSpacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LONGEST: $longest DAYS", style = MetaMono, color = Color.White.copy(alpha = .85f))
                Text("+1 TODAY", style = MetaMono, color = Color.White.copy(alpha = .85f))
            }
        }
    }
}

@Composable
private fun ReadingReflectionCard(
    articlesRead: Int,
    topCategoriesByShare: List<Pair<String, Float>>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.lg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.xl)) {
            Text(
                "ARTICLES READ",
                style = MetaMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(NewsSpacing.sm))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md),
            ) {
                Text(
                    articlesRead.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (articlesRead == 1) "article" else "articles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            if (topCategoriesByShare.isNotEmpty()) {
                Spacer(Modifier.height(NewsSpacing.lg))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(NewsSpacing.md))
                Text(
                    "WHAT YOU READ",
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(NewsSpacing.md))
                topCategoriesByShare.take(5).forEach { (name, share) ->
                    CategoryShareRow(name, share)
                    Spacer(Modifier.height(NewsSpacing.sm))
                }
            } else {
                Spacer(Modifier.height(NewsSpacing.md))
                Text(
                    "Read a few articles to see your topic mix here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryShareRow(name: String, share: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(112.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
        Spacer(Modifier.width(NewsSpacing.md))
        Text(
            text = "${(share * 100).toInt()}%",
            style = MetaMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 36.dp),
        )
    }
}