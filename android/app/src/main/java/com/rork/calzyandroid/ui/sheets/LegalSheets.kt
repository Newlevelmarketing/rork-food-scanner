package com.rork.calzyandroid.ui.sheets

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.rork.calzyandroid.data.Legal
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors

/**
 * Scrolling container for a bundled legal document.
 *
 * The text ships inside the APK, so the policy a reviewer or user reads is always
 * the one that matches this exact build. [webMirror] is optional and the row is
 * hidden when empty rather than shipping a dead link.
 */
@Composable
private fun DocumentSheet(
    open: Boolean,
    onClose: () -> Unit,
    title: String,
    body: String,
    webMirror: String,
) {
    val context = LocalContext.current
    FullScreenSheet(open = open, onClose = onClose, title = title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 40.dp),
        ) {
            CalzyCard(radius = 24.dp) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = body,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = CalzyColors.inkSoft,
                    )

                    if (webMirror.isNotBlank()) {
                        Pressable(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, webMirror.toUri()),
                                )
                            }
                        }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    text = "View online",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalzyColors.ink,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = CalzyColors.ink,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = "${Legal.APP_NAME} ${Legal.VERSION}",
                        fontSize = 12.sp,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicySheet(open: Boolean, onClose: () -> Unit, title: String) {
    DocumentSheet(
        open = open,
        onClose = onClose,
        title = title,
        body = Legal.PRIVACY_POLICY,
        webMirror = Legal.PRIVACY_POLICY_URL,
    )
}

@Composable
fun TermsOfUseSheet(open: Boolean, onClose: () -> Unit, title: String) {
    DocumentSheet(
        open = open,
        onClose = onClose,
        title = title,
        body = Legal.TERMS_OF_USE,
        webMirror = Legal.TERMS_OF_USE_URL,
    )
}
