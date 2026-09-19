package com.localstream.app.ui.screens.apps

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.localstream.app.ui.theme.TextMuted
import com.localstream.app.ui.theme.TextPrimary
import com.localstream.app.ui.theme.TextSecondary

@Composable
fun AppsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LocalStreamApplication
    val serverState by app.serverController.serverState.collectAsState()

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgBase)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    text = "Connected Ecosystem",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stream to third-party clients, players & apps",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // 1. Web Browser Portal
        item {
            AppIntegrationCard(
                icon = Icons.Default.Language,
                accentColor = EmeraldSuccess,
                title = "Web Browser Portal",
                description = "Open in Chrome, Safari, Firefox or Edge from any PC, Mac, iPhone, or Smart TV on the same Wi-Fi.",
                copyableValue = serverState.serverUrl,
                onCopy = { copyToClipboard("Web Portal URL", serverState.serverUrl) },
                instruction = "No app installation required. Includes streaming video player, audio player, and download buttons."
            )
        }

        // 2. VLC / Kodi / Infuse Network Streaming
        item {
            val vlcM3uUrl = "${serverState.serverUrl}/api/files"
            AppIntegrationCard(
                icon = Icons.Default.PlayCircleFilled,
                accentColor = PurpleNeon,
                title = "VLC / Kodi / Infuse Player",
                description = "Paste the network stream address into VLC (Media > Open Network Stream) or Kodi / Infuse.",
                copyableValue = vlcM3uUrl,
                onCopy = { copyToClipboard("VLC Media Stream URL", vlcM3uUrl) },
                instruction = "Supports hardware-accelerated MKV, MP4, FLAC playback and subtitle selection."
            )
        }

        // 3. Smart TV & Streaming Boxes
        item {
            AppIntegrationCard(
                icon = Icons.Default.Tv,
                accentColor = CyanAccent,
                title = "Smart TV & Apple TV",
                description = "Access via TV browser or DLNA Media Player on Samsung Tizen, LG webOS, Fire TV, or Apple TV.",
                copyableValue = serverState.serverUrl,
                onCopy = { copyToClipboard("Smart TV URL", serverState.serverUrl) },
                instruction = "Enable DLNA in the Renderers tab or enter the broadcast address in the TV's browser."
            )
        }

        // 4. REST API Documentation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "LocalStream HTTP API",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Built-in endpoints for developers & scripts",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ApiEndpointItem(method = "GET", endpoint = "/", description = "Responsive Web Dashboard")
                    ApiEndpointItem(method = "GET", endpoint = "/api/status", description = "Server status & client counts")
                    ApiEndpointItem(method = "GET", endpoint = "/api/files", description = "JSON catalog of all media items")
                    ApiEndpointItem(method = "GET", endpoint = "/api/search?q={query}", description = "Full-text media search")
                    ApiEndpointItem(method = "GET", endpoint = "/api/stream?id={id}", description = "HTTP 206 Partial Range Stream")
                    ApiEndpointItem(method = "GET", endpoint = "/api/download?id={id}", description = "Direct attachment download")
                    ApiEndpointItem(method = "POST", endpoint = "/api/receiver/play?id={id}", description = "Remote playback command")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppIntegrationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    copyableValue: String,
    onCopy: () -> Unit,
    instruction: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = copyableValue,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = instruction,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ApiEndpointItem(
    method: String,
    endpoint: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (method == "GET") EmeraldSuccess.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = method,
                color = if (method == "GET") EmeraldSuccess else CyanAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = endpoint,
            color = PurplePrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(170.dp)
        )
        Text(
            text = description,
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}
