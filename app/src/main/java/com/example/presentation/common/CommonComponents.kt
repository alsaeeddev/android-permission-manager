package com.example.presentation.common

import android.R
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.PermissionGroupType
import com.example.core.RiskLevel
import com.example.ui.theme.RiskCriticalColor
import com.example.ui.theme.RiskHighColor
import com.example.ui.theme.RiskLowColor
import com.example.ui.theme.RiskMediumColor

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    score: Int? = null,
    bottomCornerRound: Boolean = false,
) {
    val (bgColor, textColor, label) = when (riskLevel) {
        RiskLevel.CRITICAL -> Triple(RiskCriticalColor.copy(alpha = 0.18f), RiskCriticalColor, "Critical")
        RiskLevel.HIGH -> Triple(RiskHighColor.copy(alpha = 0.18f), RiskHighColor, "High")
        RiskLevel.MEDIUM -> Triple(RiskMediumColor.copy(alpha = 0.18f), RiskMediumColor, "Medium")
        RiskLevel.LOW -> Triple(RiskLowColor.copy(alpha = 0.18f), RiskLowColor, "Low")
    }

    val badgeShape = if (bottomCornerRound) {
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp,
            bottomStart = 8.dp
        )
    } else {
        RoundedCornerShape(8.dp)
    }

    Surface(
        color = bgColor,
        shape = badgeShape,
       // shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (score != null) "$label ($score)" else label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap = remember(packageName) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = "App Icon for $packageName",
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getPermissionGroupIcon(group: PermissionGroupType): ImageVector {
    return when (group) {
        PermissionGroupType.CAMERA -> Icons.Default.CameraAlt
        PermissionGroupType.MICROPHONE -> Icons.Default.Mic
        PermissionGroupType.LOCATION -> Icons.Default.MyLocation
        PermissionGroupType.CONTACTS -> Icons.Default.Contacts
        PermissionGroupType.SMS -> Icons.Default.Sms
        PermissionGroupType.CALL_LOG -> Icons.Default.Call
        PermissionGroupType.STORAGE -> Icons.Default.Folder
        PermissionGroupType.PHONE -> Icons.Default.Phone
        PermissionGroupType.CALENDAR -> Icons.Default.Event
        PermissionGroupType.SENSORS -> Icons.Default.Sensors
        PermissionGroupType.NEARBY_DEVICES -> Icons.Default.Bluetooth
        PermissionGroupType.NOTIFICATIONS -> Icons.Default.Notifications
        PermissionGroupType.SPECIAL -> Icons.Default.Security
        PermissionGroupType.OTHER -> Icons.Default.MoreHoriz
    }
}
