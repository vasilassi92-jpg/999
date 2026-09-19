package com.localstream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localstream.app.domain.model.DeviceType
import com.localstream.app.domain.model.RendererDevice
import com.localstream.app.ui.theme.BorderSubtle
import com.localstream.app.ui.theme.CyanAccent
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
fun RendererDeviceCard(
    device: RendererDevice,
    onConnectToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, typeLabel, accentColor) = when (device.deviceType) {
        DeviceType.SMART_TV -> Triple(Icons.Default.Tv, "Smart TV", PurplePrimary)
        DeviceType.CHROMECAST -> Triple(Icons.Default.Cast, "Chromecast", CyanAccent)
        DeviceType.DLNA_RENDERER -> Triple(Icons.Default.CastConnected, "DLNA Renderer", PurpleNeon)
        DeviceType.UPNP_MEDIA_SERVER -> Triple(Icons.Default.Computer, "Media Server", PurpleSecondary)
        DeviceType.PHONE_RECEIVER -> Triple(Icons.Default.PhoneAndroid, "Local Receiver", EmeraldSuccess)
        DeviceType.COMPUTER -> Triple(Icons.Default.Computer, "Network PC", CyanAccent)
        DeviceType.GENERIC -> Triple(Icons.Default.Cast, "LAN Device", TextSecondary)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (device.isConnected) accentColor.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("renderer_card_${device.id}"),
        shape = RoundedCornerShape(16.dp),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkBgCard)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${device.ipAddress}:${device.port}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onConnectToggle,
                modifier = Modifier.testTag("connect_renderer_${device.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.isConnected) EmeraldSuccess.copy(alpha = 0.2f) else PurpleSecondary,
                    contentColor = if (device.isConnected) EmeraldSuccess else TextPrimary
                )
            ) {
                Text(
                    text = if (device.isConnected) "Connected" else "Cast",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
