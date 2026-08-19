package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.AiService
import com.rork.calzyandroid.data.EntrySource
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.LocalAppLanguage
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.navigation.MealDraft
import com.rork.calzyandroid.ui.theme.CalzyColors
import kotlinx.coroutines.launch

private val suggestions = listOf(
    "Chicken caesar salad with dressing",
    "Two eggs, toast and butter",
    "Big bowl of spaghetti bolognese",
    "Protein shake with banana",
    "Sushi set, 8 pieces",
    "Oatmeal with berries and honey",
)

/** Describe a meal in words and let the AI estimate its nutrition. */
@Composable
fun DescribeSheet(
    open: Boolean,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onResult: (MealDraft) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    var text by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(open) {
        if (!open) {
            text = ""
            analyzing = false
            error = null
        }
    }

    fun analyze() {
        val description = text.trim()
        if (description.isEmpty() || analyzing) return
        analyzing = true
        error = null
        scope.launch {
            try {
                val result = AiService.analyzeText(
                    description = description,
                    jesterMode = data.profile.jesterMode,
                    languageName = language.englishName,
                )
                analyzing = false
                onResult(MealDraft(result = result, source = EntrySource.text))
            } catch (failure: Exception) {
                analyzing = false
                error = AiService.messageFor(failure)
            }
        }
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = "Describe your meal",
        footer = {
            if (analyzing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = CalzyColors.ink,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "  Estimating nutrition…",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                    )
                }
            } else {
                PrimaryButton(
                    text = "Estimate nutrition",
                    onClick = { analyze() },
                    enabled = text.trim().isNotEmpty(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalzyCard(radius = 22.dp) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = "e.g. grilled salmon with rice and a side salad…",
                            color = CalzyColors.inkFaint,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            error?.let { message ->
                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.protein,
                )
            }

            Text(
                text = "IDEAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = CalzyColors.inkFaint,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.height(220.dp),
                userScrollEnabled = false,
            ) {
                items(suggestions) { suggestion ->
                    Pressable(onClick = { text = suggestion }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.78f))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = CalzyColors.plum,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CalzyColors.inkSoft,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

