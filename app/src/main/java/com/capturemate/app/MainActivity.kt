package com.capturemate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.ui.theme.CaptureMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaptureMateTheme {
                HomeRoute()
            }
        }
    }
}
