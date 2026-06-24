package com.hasanege.materialtv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.hasanege.materialtv.ui.screens.settings.CustomizationScreen
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomizationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTVTheme {
                CustomizationScreen(onBackClick = { finish() })
            }
        }
    }
}
