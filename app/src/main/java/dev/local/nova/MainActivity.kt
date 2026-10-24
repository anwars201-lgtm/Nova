package dev.local.nova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.local.nova.ui.chat.ChatScreen
import dev.local.nova.ui.home.HomeScreen
import dev.local.nova.ui.settings.SettingsScreen
import dev.local.nova.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }
                var threadId by rememberSaveable { mutableStateOf<String?>(null) }

                when (route) {
                    ROUTE_SETTINGS -> SettingsScreen(onBack = { route = ROUTE_HOME })

                    ROUTE_CHAT -> ChatScreen(
                        threadId = threadId.orEmpty(),
                        onBack = {
                            threadId = null
                            route = ROUTE_HOME
                        },
                        onOpenSettings = { route = ROUTE_SETTINGS },
                    )

                    else -> HomeScreen(
                        onOpenSettings = { route = ROUTE_SETTINGS },
                        onOpenThread = { id ->
                            threadId = id
                            route = ROUTE_CHAT
                        },
                    )
                }
            }
        }
    }

    private companion object {
        const val ROUTE_HOME = "home"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_CHAT = "chat"
    }
}
