package dev.local.nova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.local.nova.ui.home.HomeScreen
import dev.local.nova.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                HomeScreen()
            }
        }
    }
}
