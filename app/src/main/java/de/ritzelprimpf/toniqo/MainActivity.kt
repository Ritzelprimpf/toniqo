package de.ritzelprimpf.toniqo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import de.ritzelprimpf.toniqo.ui.MainScreen
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToniqoTheme {
                MainScreen()
            }
        }
    }
}
