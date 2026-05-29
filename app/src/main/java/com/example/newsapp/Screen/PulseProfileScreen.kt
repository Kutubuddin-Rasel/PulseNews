package com.example.newsapp.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.PulseProfileViewModel
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.theme.AccentGradient
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseProfileScreen(viewModel: PulseProfileViewModel = hiltViewModel()) {
    val p by viewModel.profile.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val badges = remember(p) { allBadges(p.totalArticlesRead, p.categoryReadCounts) }

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding)
                    .padding(horizontal = NewsSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                contentPadding = PaddingValues(top = NewsSpacing.lg, bottom = NewsSpacing.xxl),
            ) {
                item(span = { GridItemSpan(2) }) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Profile", style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(NewsSpacing.xs))
                        Text("READER SINCE JAN 2025", style = MetaMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(NewsSpacing.lg))
                        
                        AuthCard(user, onSignIn = viewModel::signIn, onSignOut = viewModel::signOut)
                        
                        Spacer(Modifier.height(NewsSpacing.lg))
                        StreakCard(current = p.currentStreak, longest = p.longestStreak)
                        Spacer(Modifier.height(NewsSpacing.sm))
                    }
                }
                item { StatCard("ARTICLES READ", p.totalArticlesRead.toString(), "All time") }
                item { StatCard("THIS WEEK", "23", "+4 vs last") }
                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(NewsSpacing.md))
                    Text("Badges", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(NewsSpacing.sm))
                }
                items(badges) { b -> BadgeTile(b) }
            }
        }
    }
}

@Composable
private fun AuthCard(user: com.google.firebase.auth.FirebaseUser?, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            if (user != null) {
                Text(text = "Signed in as", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(text = user.displayName ?: user.email ?: "Unknown User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(NewsSpacing.sm))
                Text(text = "Saved articles and preferences are synced.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(NewsSpacing.md))
                Button(onClick = onSignOut, shape = RoundedCornerShape(NewsRadius.pill)) {
                    Text("Sign Out")
                }
            } else {
                Text(text = "Not Signed In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(NewsSpacing.sm))
                Text(text = "Sign in to sync your saved articles across devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(NewsSpacing.md))
                Button(onClick = onSignIn, shape = RoundedCornerShape(NewsRadius.pill)) {
                    Text("Sign in with Google")
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
private fun StatCard(label: String, value: String, sub: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.lg)) {
            Text(label, style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(NewsSpacing.xs))
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BadgeTile(b: BadgeData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (b.unlocked) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        shape = RoundedCornerShape(NewsRadius.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.md).alpha(if (b.unlocked) 1f else 0.45f)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(NewsRadius.sm))
                    .background(if (b.unlocked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .border(if (b.unlocked) 0.dp else 1.dp,
                        MaterialTheme.colorScheme.outline, RoundedCornerShape(NewsRadius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Text(b.glyph, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(NewsSpacing.sm))
            Text(b.name, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(b.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class BadgeData(val glyph: String, val name: String, val description: String, val unlocked: Boolean)

private fun allBadges(total: Int, categories: Map<String, Int>): List<BadgeData> = listOf(
    BadgeData("\u2728", "First Steps", "Read your first article", total >= 1),
    BadgeData("\uD83D\uDCD6", "Avid Reader", "Read 10 articles", total >= 10),
    BadgeData("\uD83D\uDCF0", "News Junkie", "Read 50 articles", total >= 50),
    BadgeData("\uD83C\uDFC6", "Centurion", "Read 100 articles", total >= 100),
    BadgeData("\uD83D\uDCBB", "Tech Guru", "10 Tech articles", (categories["tech"] ?: 0) >= 10),
    BadgeData("\u26BD", "Sports Fan", "10 Sports articles", (categories["sports"] ?: 0) >= 10),
)

private fun Modifier.alpha(a: Float) = this.then(Modifier.graphicsLayer(alpha = a))