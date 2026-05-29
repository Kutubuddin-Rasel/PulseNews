package com.example.newsapp.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.PulseProfileViewModel
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.tokens.NewsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseProfileScreen(viewModel: PulseProfileViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsState()

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Your Profile", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = NewsSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(NewsSpacing.lg)
            ) {
                // Streaks Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md)
                ) {
                    // Current Streak
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(NewsSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Fire",
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(NewsSpacing.sm))
                            Text(
                                text = "${profile.currentStreak}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Day Streak",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Stats
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.md)
                    ) {
                        StatBox(title = "Longest Streak", value = "${profile.longestStreak} days")
                        StatBox(title = "Total Read", value = "${profile.totalArticlesRead} articles")
                    }
                }

                Spacer(modifier = Modifier.height(NewsSpacing.md))

                // Badges Section
                Text(
                    text = "Unlocked Badges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val badges = getBadges(profile.totalArticlesRead, profile.categoryReadCounts)

                if (badges.isEmpty()) {
                    Text(
                        text = "Read more articles to unlock badges!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.md)
                    ) {
                        items(badges) { badge ->
                            BadgeCard(badgeName = badge.name, description = badge.description)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(NewsSpacing.md)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BadgeCard(badgeName: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(NewsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Badge",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(NewsSpacing.sm))
            Text(
                text = badgeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class Badge(val name: String, val description: String)

fun getBadges(totalRead: Int, categoryCounts: Map<String, Int>): List<Badge> {
    val badges = mutableListOf<Badge>()
    if (totalRead >= 1) badges.add(Badge("First Steps", "Read your first article"))
    if (totalRead >= 10) badges.add(Badge("Avid Reader", "Read 10 articles"))
    if (totalRead >= 50) badges.add(Badge("News Junkie", "Read 50 articles"))
    
    // Category badges
    val tech = categoryCounts["tech"] ?: 0
    if (tech >= 10) badges.add(Badge("Tech Guru", "Read 10 Tech articles"))
    
    val sports = categoryCounts["sports"] ?: 0
    if (sports >= 10) badges.add(Badge("Sports Fan", "Read 10 Sports articles"))

    return badges
}
