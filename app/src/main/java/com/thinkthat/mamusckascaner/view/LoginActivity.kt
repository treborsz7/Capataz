package com.thinkthat.mamusckascaner.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.codegalaxy.barcodescanner.view.LoginScreen
import com.thinkthat.mamusckascaner.R
import com.thinkthat.mamusckascaner.di.ViewModelFactories
import com.thinkthat.mamusckascaner.presentation.login.LoginEvent
import com.thinkthat.mamusckascaner.presentation.login.LoginPantalla
import com.thinkthat.mamusckascaner.presentation.login.LoginViewModel
import com.thinkthat.mamusckascaner.ui.theme.BarCodeScannerTheme

/**
 * Pantalla de entrada. El auto-login y la validación viven en [LoginViewModel];
 * acá solo queda el splash, el tema y la navegación a [MainActivity].
 */
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels { ViewModelFactories.login }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener el splash visible un momento antes de cambiar de tema
        Thread.sleep(SPLASH_MILLIS)
        setTheme(R.style.Theme_BarCodeScanner)

        enableEdgeToEdge()
        viewModel.iniciar()

        setContent {
            BarCodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.events.collect { evento ->
                            when (evento) {
                                LoginEvent.LoginExitoso -> irAlMenu()
                            }
                        }
                    }

                    when (state.pantalla) {
                        // Mientras se resuelve el auto-login no se muestra el formulario.
                        LoginPantalla.AUTENTICANDO -> Unit

                        LoginPantalla.FORMULARIO -> LoginScreen(
                            state = state,
                            onUsuarioChange = viewModel::onUsuarioChange,
                            onContrasenaChange = viewModel::onContrasenaChange,
                            onRecordarChange = viewModel::onRecordarChange,
                            onLogin = viewModel::login,
                            onDismissError = viewModel::limpiarError
                        )
                    }
                }
            }
        }
    }

    private fun irAlMenu() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private companion object {
        const val SPLASH_MILLIS = 2000L
    }
}
