package de.ritzelprimpf.toniqo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToniqoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Phase1Placeholder(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun Phase1Placeholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.phase_1_placeholder),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
