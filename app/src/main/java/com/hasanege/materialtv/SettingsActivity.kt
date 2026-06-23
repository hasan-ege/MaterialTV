package com.hasanege.materialtv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.appcompat.app.AppCompatActivity
import com.hasanege.materialtv.ui.screens.settings.SettingsScreen
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTVTheme {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}
