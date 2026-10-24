package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Новый плейлист",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Введите название для вашего нового плейлиста",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Название плейлиста") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistName.trim().isNotBlank()) {
                        onCreate(playlistName.trim())
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                modifier = Modifier.testTag("create_playlist_confirm")
            ) {
                Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}
