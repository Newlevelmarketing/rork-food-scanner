package com.rork.calzyandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rork.calzyandroid.data.FoodDb
import com.rork.calzyandroid.data.I18n
import com.rork.calzyandroid.ui.navigation.AppNavigation
import com.rork.calzyandroid.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bundled catalogues: 32-language strings and the offline food table.
        I18n.load(applicationContext)
        FoodDb.load(applicationContext)

        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
