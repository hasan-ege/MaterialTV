package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.hasanege.materialtv.ui.screens.settings.CustomizationScreen
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomizationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToMain()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            MaterialTVTheme {
                CustomizationScreen(onBackClick = { navigateBackToMain() })
            }
        }
    }

    private fun navigateBackToMain() {
        if (isTaskRoot) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        finish()
    }
}
