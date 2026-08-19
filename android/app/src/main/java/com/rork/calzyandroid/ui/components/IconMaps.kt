package com.rork.calzyandroid.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Rowing
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsMma
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.rork.calzyandroid.data.EntrySource
import com.rork.calzyandroid.data.MealSlot

/** Icon lookups shared across screens. */
object IconMaps {

    fun source(source: EntrySource): ImageVector = when (source) {
        EntrySource.photo -> Icons.Outlined.CameraAlt
        EntrySource.text -> Icons.Outlined.Keyboard
        EntrySource.search -> Icons.Outlined.Search
        EntrySource.saved -> Icons.Outlined.Bookmark
        EntrySource.manual -> Icons.Outlined.Edit
    }

    fun slot(slot: MealSlot): ImageVector = when (slot) {
        MealSlot.breakfast -> Icons.Outlined.WbTwilight
        MealSlot.lunch -> Icons.Outlined.WbSunny
        MealSlot.dinner -> Icons.Outlined.DarkMode
        MealSlot.snack -> Icons.Outlined.LocalCafe
    }

    fun exercise(key: String): ImageVector = when (key) {
        "walk" -> Icons.AutoMirrored.Outlined.DirectionsWalk
        "run" -> Icons.AutoMirrored.Outlined.DirectionsRun
        "bike" -> Icons.AutoMirrored.Outlined.DirectionsBike
        "weights" -> Icons.Outlined.FitnessCenter
        "hiit" -> Icons.Outlined.Bolt
        "swim" -> Icons.Outlined.Pool
        "yoga" -> Icons.Outlined.SelfImprovement
        "football" -> Icons.Outlined.SportsSoccer
        "tennis" -> Icons.Outlined.SportsTennis
        "row" -> Icons.Outlined.Rowing
        "boxing" -> Icons.Outlined.SportsMma
        "hike" -> Icons.Outlined.Hiking
        else -> Icons.Outlined.FitnessCenter
    }
}
