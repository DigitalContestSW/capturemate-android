package com.capturemate.app.feature.debugocr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.capturemate.app.ui.theme.CaptureMateTheme

class DebugOcrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaptureMateTheme {
                DebugOcrRoute()
            }
        }
    }
}
