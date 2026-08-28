// src/main/java/com/codegalaxy/barcodescanner/view/LoginScreen.kt
package com.codegalaxy.barcodescanner.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import com.thinkthat.mamusckascaner.presentation.login.LoginUiState
import com.thinkthat.mamusckascaner.view.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsuarioChange: (String) -> Unit = {},
    onContrasenaChange: (String) -> Unit = {},
    onRecordarChange: (Boolean) -> Unit = {},
    onLogin: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val usuario = state.usuario
    val contrasena = state.contrasena
    val recordar = state.recordar
    val isLoading = state.isLoading
    val errorMessage = state.error

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive values
    val horizontalPadding = maxOf(minOf(screenWidth * 0.08f, 32.dp), 16.dp)
    val logoSize = maxOf(minOf(screenWidth * 0.8f, 450.dp), 300.dp)
    val formWidth = 0.8f
    val titleFontSize = maxOf(minOf((screenWidth * 0.06f).value, 28f), 20f).sp
    val bodyFontSize = maxOf(minOf((screenWidth * 0.04f).value, 18f), 14f).sp
    val buttonHeight = maxOf(minOf(screenHeight * 0.07f, 64.dp), 48.dp)
    val topOffset = -maxOf(minOf(screenHeight * 0.05f, 50.dp), 30.dp)
    val buttonWidth = maxOf(minOf(screenWidth * 0.6f, 250.dp), 150.dp)
    

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCD0914))
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Título centrado en la parte superior
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = horizontalPadding / 2)
        ) {
            Text(
                text = "Login",
                fontSize = titleFontSize,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((screenHeight * 0.02f).coerceAtLeast(12.dp).coerceAtMost(20.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .offset(y = topOffset)
                .align(Alignment.Center)
        ) {
            android.util.Log.d("LoginScreen", "Rendering login form")
            
            // Logo centrado
            Icon(
                painter = painterResource(id = com.thinkthat.mamusckascaner.R.drawable.logos_y__1__05__1_),
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(logoSize)
            )
            
            // Usuario editable
            OutlinedTextField(
                value = usuario,
                onValueChange = onUsuarioChange,
                label = { Text("Usuario", color = Color.White) },
                placeholder = { Text("Ingrese su usuario", color = Color.White) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(formWidth),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White
                )
            )
            
            // Contraseña
            OutlinedTextField(
                value = contrasena,
                onValueChange = onContrasenaChange,
                label = { Text("Contraseña", color = Color.White) },
                placeholder = { Text("Ingrese su contraseña", color = Color.White) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(formWidth),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White
                )
            )

            // Checkbox Recordar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(formWidth)
            ) {
                Checkbox(
                    checked = recordar,
                    onCheckedChange = onRecordarChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recordar", fontSize = bodyFontSize, color = Color.White)
            }
        } // Cierre del Column

        // Botón Identificar en posición fija
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Button(
                onClick = onLogin,
                enabled = state.puedeIniciarSesion,
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Identificar", color = Color.Black, fontSize = bodyFontSize)
                }
            }
        }

        // Mostrar mensaje de error si existe (superpuesto sobre el botón)
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                ErrorMessage(
                    message = errorMessage,
                    modifier = Modifier.fillMaxWidth(formWidth),
                    onDismiss = onDismissError
                )
            }
        }
    }
}
