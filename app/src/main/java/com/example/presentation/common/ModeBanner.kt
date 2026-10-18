package com.example.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.domain.model.ControllerMode
import com.example.domain.model.ModeStatus
import com.example.ui.theme.PrivilegedAccentColor
import com.example.ui.theme.RiskLowColor
import com.example.ui.theme.RiskMediumColor

@Composable
fun ModeBanner(
    modeStatus: ModeStatus,
    onRequestShizukuPermission: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrivileged = modeStatus.mode == ControllerMode.SHIZUKU_PRIVILEGED

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mode_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPrivileged) Icons.Default.Bolt else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPrivileged) PrivilegedAccentColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPrivileged) "Shizuku Privileged Mode" else "Standard Settings Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (isPrivileged) RiskLowColor.copy(alpha = 0.15f) else RiskMediumColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isPrivileged) "Direct Control" else "Redirects Only",
                        color = if (isPrivileged) RiskLowColor else RiskMediumColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = modeStatus.message,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isPrivileged) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (modeStatus.isShizukuRunning && !modeStatus.hasShizukuPermission) {
                        FilledTonalButton(
                            onClick = onRequestShizukuPermission,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PrivilegedAccentColor.copy(alpha = 0.2f),
                                contentColor = PrivilegedAccentColor
                            ),
                            modifier = Modifier.testTag("btn_grant_shizuku")
                        ) {
                            Text("Authorize Shizuku", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    FilledTonalButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.testTag("btn_setup_guide")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setup Guide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
