package com.localstream.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localstream.app.LocalStreamApplication
import com.localstream.app.ui.theme.BorderSubtle
import com.localstream.app.ui.theme.CyanAccent
import com.localstream.app.ui.theme.DarkBgBase
import com.localstream.app.ui.theme.DarkBgCard
import com.localstream.app.ui.theme.DarkBgSurface
import com.localstream.app.ui.theme.EmeraldSuccess
import com.localstream.app.ui.theme.PurpleNeon
import com.localstream.app.ui.theme.PurplePrimary
import com.localstream.app.ui.theme.PurpleSecondary
import com.localstream.app.ui.theme.RoseError
import com.localstream.app.ui.theme.TextMuted
import com.localstream.app.ui.theme.TextPrimary
import com.localstream.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LocalStreamApplication
    val scope = rememberCoroutineScope()

    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = com.localstream.app.data.preferences.AppSettings())

    var showPortDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // SAF directory picker
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            scope.launch {
                app.settingsRepository.addSelectedFolder(uri.toString())
                Toast.makeText(context, "Folder added to indexed paths", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgBase)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Preferences",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Server, Security & Streaming Configuration",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section 1: Server Configuration
        item {
            SettingsCategoryHeader("Server Configuration", Icons.Default.PowerSettingsNew, PurplePrimary)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Server Port
                    SettingsRowClickable(
                        title = "Server Port",
                        subtitle = "Current listening port: ${settings.serverPort}",
                        onClick = { showPortDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-start on Launch
                    SettingsRowToggle(
                        title = "Auto-start on Launch",
                        subtitle = "Launch HTTP server whenever LocalStream opens",
                        isChecked = settings.autoStart,
                        onCheckedChange = { scope.launch { app.settingsRepository.setAutoStart(it) } }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stop on Wi-Fi Disconnect
                    SettingsRowToggle(
                        title = "Stop on Wi-Fi Disconnect",
                        subtitle = "Automatically shutdown server when leaving Wi-Fi",
                        isChecked = settings.stopOnWifiDisconnect,
                        onCheckedChange = { scope.launch { app.settingsRepository.setStopOnWifiDisconnect(it) } }
                    )
                }
            }
        }

        // Section 2: Security & Privacy
        item {
            SettingsCategoryHeader("Security & Access", Icons.Default.Security, CyanAccent)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRowToggle(
                        title = "Require Password",
                        subtitle = "Require authentication before serving web clients",
                        isChecked = settings.requirePassword,
                        onCheckedChange = { isReq ->
                            scope.launch {
                                app.settingsRepository.setRequirePassword(isReq)
                                app.localHttpServer.requirePassword = isReq
                            }
                        }
                    )

                    if (settings.requirePassword) {
                        Spacer(modifier = Modifier.height(14.dp))
                        SettingsRowClickable(
                            title = "Set Server Password",
                            subtitle = if (settings.isPasswordSet) "Password configured" else "No password set",
                            onClick = { showPasswordDialog = true }
                        )
                    }
                }
            }
        }

        // Section 3: Storage & Library
        item {
            SettingsCategoryHeader("Storage & Indexing", Icons.Default.Storage, EmeraldSuccess)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRowClickable(
                        title = "Add Shared Folder",
                        subtitle = "Select an additional folder via Storage Access Framework",
                        onClick = { folderPicker.launch(null) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowClickable(
                        title = "Clear Media Index",
                        subtitle = "Remove indexed metadata cache from local database",
                        isDestructive = true,
                        onClick = { showClearConfirm = true }
                    )
                }
            }
        }

        // Section 4: Appearance
        item {
            SettingsCategoryHeader("Appearance", Icons.Default.ColorLens, PurpleNeon)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRowToggle(
                        title = "AMOLED Pure Black",
                        subtitle = "Deep true-black backgrounds for OLED displays",
                        isChecked = settings.amoledMode,
                        onCheckedChange = { scope.launch { app.settingsRepository.setAmoledMode(it) } }
                    )
                }
            }
        }

        // About Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("LocalStream Android", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version 1.0.0 • Production Build", color = PurplePrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Local-network media streaming, UPnP/DLNA discovery, and HTTP Range playback engine.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Port Dialog
    if (showPortDialog) {
        var portText by remember { mutableStateOf(settings.serverPort.toString()) }
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            containerColor = DarkBgSurface,
            title = { Text("Set Server Port", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port (1024 - 65535)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = portText.toIntOrNull()
                        if (parsed != null && parsed in 1024..65535) {
                            scope.launch {
                                app.settingsRepository.setServerPort(parsed)
                                Toast.makeText(context, "Port set to $parsed", Toast.LENGTH_SHORT).show()
                                showPortDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Enter a valid port (1024-65535)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                ) {
                    Text("Save", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Password Dialog
    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = DarkBgSurface,
            title = { Text("Set Server Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length >= 4) {
                            scope.launch {
                                app.settingsRepository.setServerPassword(newPass)
                                Toast.makeText(context, "Server password updated", Toast.LENGTH_SHORT).show()
                                showPasswordDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary)
                ) {
                    Text("Save", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Clear Index Confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = DarkBgSurface,
            title = { Text("Clear Media Index?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will clear the database index of all media files. Your actual files on disk will NOT be deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            app.fileRepository.deleteAllFiles()
                            Toast.makeText(context, "Media database index cleared", Toast.LENGTH_SHORT).show()
                            showClearConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Clear Index", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String, icon: ImageVector, tint: Color) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingsRowClickable(
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDestructive) RoseError else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsRowToggle(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PurplePrimary,
                checkedTrackColor = PurpleSecondary.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBgCard
            )
        )
    }
}
