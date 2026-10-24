package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LeafRepository
import com.example.ui.components.ServerUrlDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HeartRed
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: LeafRepository,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val user by repository.currentUser.collectAsState()
    val playlists by repository.playlists.collectAsState()
    val likedTracks by repository.likedTracks.collectAsState()
    val isSyncing by repository.isSyncing.collectAsState()

    var showServerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
        label = "rotateSync"
    )

    if (showServerDialog) {
        ServerUrlDialog(
            currentUrl = repository.getBaseUrl(),
            onDismiss = { showServerDialog = false },
            onSave = { newUrl ->
                repository.setBaseUrl(newUrl)
                showServerDialog = false
                scope.launch { repository.syncLibrary() }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Выйти из аккаунта?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Сессия будет завершена. Для повторного доступа потребуется вход.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            repository.logout()
                            onLogoutSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("logout_confirm")
                ) {
                    Text("Выйти", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        // User Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = LeafGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.username?.takeIf { it.isNotBlank() } ?: user?.login ?: "Пользователь",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (user?.verified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Верифицирован",
                                tint = LeafGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${user?.login ?: "guest"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Library Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${likedTracks.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text(text = "Любимых", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${playlists.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text(text = "Плейлистов", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Server Connection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Сервер Leaf Web", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = repository.getBaseUrl(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showServerDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_change_server")
                ) {
                    Text("Изменить адрес сервера", color = LeafGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sync Action Card
        Button(
            onClick = {
                scope.launch { repository.syncLibrary() }
            },
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_sync_library")
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = LeafGreen,
                modifier = Modifier
                    .size(20.dp)
                    .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Синхронизация..." else "Синхронизировать библиотеку",
                color = LeafGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_logout_button")
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выйти из аккаунта", color = ErrorRed, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
