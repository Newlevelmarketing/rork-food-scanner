package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.I18n
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.LocalAppLanguage
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors

/** Language picker with search across English names and endonyms. */
@Composable
fun LanguageSheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val t = LocalT.current
    val active = LocalAppLanguage.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }

    LaunchedEffect(open) {
        if (!open) query = ""
    }

    val results = remember(query) {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) {
            I18n.languages
        } else {
            I18n.languages.filter {
                it.englishName.lowercase().contains(trimmed) ||
                    it.nativeName.lowercase().contains(trimmed)
            }
        }
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = t("l.title"),
        scrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CalzyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                radius = 18.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = CalzyColors.inkFaint,
                        modifier = Modifier.size(17.dp),
                    )
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(t("l.search"), color = CalzyColors.inkFaint) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                    )
                }
            }

            Text(
                text = t("l.note"),
                fontSize = 12.sp,
                color = CalzyColors.inkFaint,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                lineHeight = 16.sp,
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
            ) {
                items(results, key = { it.code }) { language ->
                    val selected = language.code == active.code
                    Pressable(onClick = {
                        viewModel.setProfile { it.copy(languageCode = language.code) }
                        onClose()
                    }) {
                        CalzyCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                            ) {
                                Text(text = language.flag, fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = language.nativeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CalzyColors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = language.englishName,
                                        fontSize = 12.sp,
                                        color = CalzyColors.inkFaint,
                                    )
                                }
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(CalzyColors.mint),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
