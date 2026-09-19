package com.localstream.app.ui.screens.renderers

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localstream.app.LocalStreamApplication
import com.localstream.app.domain.model.DeviceType
import com.localstream.app.domain.model.RendererDevice
import com.localstream.app.domain.model.SmbShare
import com.localstream.app.network.NetworkUtils
import com.localstream.app.ui.components.RendererDeviceCard
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
fun RenderersScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LocalStreamApplication
    val scope = rememberCoroutineScope()

    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = com.localstream.app.data.preferences.AppSettings())
    val isDiscovering by app.deviceDiscovery.isDiscovering.collectAsState()
    val discoveredRenderers by app.deviceDiscovery.discoveredRenderers.collectAsState()
    val savedSmbShares by app.smbManager.savedShares.collectAsState()

    var showSmbDialog by remember { mutableStateOf(false) }
    var connectedDeviceId by remember { mutableStateOf<String?>(null) }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Streaming & Renderers",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "LAN Casting, DLNA & Network Shares",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            app.deviceDiscovery.discoverAll(includeSubnetScan = true)
                            Toast.makeText(context, "Network scan complete", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("scan_renderers_button")
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Scan Network",
                            tint = PurplePrimary
                        )
                    }
                }
            }
        }

        // Phone as Receiver Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (settings.receiverEnabled) EmeraldSuccess.copy(alpha = 0.5f) else BorderSubtle,
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (settings.receiverEnabled) EmeraldSuccess.copy(alpha = 0.15f) else DarkBgCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (settings.receiverEnabled) EmeraldSuccess else TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Phone as Receiver",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (settings.receiverEnabled) "Accepting remote streams" else "Receiver inactive",
                                    color = if (settings.receiverEnabled) EmeraldSuccess else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = settings.receiverEnabled,
                            onCheckedChange = { isEnabled ->
                                scope.launch {
                                    app.settingsRepository.setReceiverEnabled(isEnabled)
                                    if (isEnabled && !app.localHttpServer.isRunning.value) {
                                        app.serverController.startServer(settings.serverPort)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldSuccess,
                                checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBgCard
                            ),
                            modifier = Modifier.testTag("receiver_toggle_switch")
                        )
                    }

                    if (settings.receiverEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val ip = NetworkUtils.getLocalIpAddress()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBgCard)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Receiver Active Endpoint", color = TextMuted, fontSize = 10.sp)
                                    Text(
                                        text = "http://$ip:${settings.serverPort}/api/receiver/play",
                                        color = EmeraldSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Available Renderers Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Devices (${discoveredRenderers.size})",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        scope.launch {
                            app.deviceDiscovery.discoverAll(includeSubnetScan = true)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCard)
                ) {
                    Text("Scan LAN", color = PurplePrimary, fontSize = 12.sp)
                }
            }
        }

        // Renderers List
        if (discoveredRenderers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBgSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = PurplePrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No DLNA or Casting devices discovered yet",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure your Smart TV, Chromecast or PC is on this Wi-Fi network",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(discoveredRenderers) { dev ->
                val isConnected = connectedDeviceId == dev.id
                RendererDeviceCard(
                    device = dev.copy(isConnected = isConnected),
                    onConnectToggle = {
                        connectedDeviceId = if (isConnected) null else dev.id
                        val state = if (isConnected) "Disconnected from" else "Connected to"
                        Toast.makeText(context, "$state ${dev.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // SMB Network Shares Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMB Network Shares",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showSmbDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCard),
                    modifier = Modifier.testTag("add_smb_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add SMB", color = CyanAccent, fontSize = 12.sp)
                }
            }
        }

        // SMB Shares List
        if (savedSmbShares.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBgSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No SMB shares configured. Connect a Windows PC or NAS share.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(savedSmbShares) { share ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Computer, null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${share.server}/${share.shareName}",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "User: ${share.username.ifEmpty { "Guest" }} • Path: ${share.folderPath.ifEmpty { "/" }}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val reachable = app.smbManager.testConnection(share.server)
                                    val msg = if (reachable) "Connected to ${share.server} successfully" else "Unable to reach ${share.server}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBgCard)
                        ) {
                            Text("Test", color = TextPrimary, fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { app.smbManager.removeShare(share.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = RoseError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add SMB Share Dialog
    if (showSmbDialog) {
        SmbConnectionDialog(
            onDismiss = { showSmbDialog = false },
            onSave = { server, user, pass, share, folder ->
                scope.launch {
                    app.smbManager.saveShare(server, user, pass, share, folder)
                    Toast.makeText(context, "Saved SMB share for $server", Toast.LENGTH_SHORT).show()
                    showSmbDialog = false
                }
            }
        )
    }
}

@Composable
private fun SmbConnectionDialog(
    onDismiss: () -> Unit,
    onSave: (server: String, user: String, pass: String, share: String, folder: String) -> Unit
) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var folderPath by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBgSurface,
        title = {
            Text(text = "Connect to SMB Share", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server (IP or Hostname)") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth().testTag("smb_server_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("smb_user_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("smb_password_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )

                OutlinedTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    label = { Text("Share Name") },
                    placeholder = { Text("Media") },
                    modifier = Modifier.fillMaxWidth().testTag("smb_share_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )

                OutlinedTextField(
                    value = folderPath,
                    onValueChange = { folderPath = it },
                    label = { Text("Subfolder (optional)") },
                    placeholder = { Text("/Movies") },
                    modifier = Modifier.fillMaxWidth().testTag("smb_folder_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (server.isNotBlank() && shareName.isNotBlank()) {
                        onSave(server, username, password, shareName, folderPath)
                    }
                },
                enabled = server.isNotBlank() && shareName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleSecondary),
                modifier = Modifier.testTag("smb_dialog_save_button")
            ) {
                Text("Connect & Save", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
