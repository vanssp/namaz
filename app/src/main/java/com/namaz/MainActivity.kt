package com.namaz

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.namaz.ui.NamazRoot
import com.namaz.ui.theme.NamazTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) {
            window.decorView.isForceDarkAllowed = false
        }
        setContent {
            NamazTheme {
                NamazRoot()
            }
        }
    }
}
