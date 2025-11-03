// src/main/java/com/codegalaxy/barcodescanner/view/LoginScreen.kt
package com.codegalaxy.barcodescanner.view

import android.content.SharedPreferences
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
import com.thinkthat.mamusckascaner.view.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    empresas: List<Pair<String, String>> = emptyList(),
    empresaSeleccionada: String = "",
    onEmpresaSeleccionada: (String) -> Unit = {},
    deposito: String = "",
    onDepositoChange: (String) -> Unit = {},
    onLogin: (String, String, Boolean, String) -> Unit,
    savedUser: String = "",
    savedPass: String = "",
    savedRemember: Boolean = false,
    savedEmpresa: String = "31",
    prefs: SharedPreferences?
) {
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
    
    android.util.Log.d("LoginScreen", "LoginScreen composable is being rendered")
    var usuario by remember { mutableStateOf(savedUser) }
    var contrasena by remember { mutableStateOf(savedPass) }
    var recordar by remember { mutableStateOf(savedRemember) }
    var empresa by remember { mutableStateOf(savedEmpresa) }
    var expanded by remember { mutableStateOf(false) }

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
            
            // Si ya se identificó (hay empresas), mostrar solo labels
            if (empresas.isNotEmpty()) {
                // Usuario como label (no editable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(formWidth)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Usuario: $usuario",
                            color = Color.White,
                            fontSize = bodyFontSize
                        )
                    }
                }
            } else {
                // Usuario editable
                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        android.util.Log.d("LoginScreen", "User input updated: $usuario")
                    },
                    label = { Text("Usuario", color = Color.White) },
                    placeholder = { Text("Ingrese su usuario", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(formWidth),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    )
                )
            }
            
            // Contraseña - solo mostrar si no hay empresas (no identificado aún)
            if (empresas.isEmpty()) {
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                        android.util.Log.d("LoginScreen", "Password input updated")
                    },
                    label = { Text("Contraseña", color = Color.White) },
                    placeholder = { Text("Ingrese su contraseña", color = Color.White) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(formWidth),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    )
                )
            }
            
            // Mostrar mensaje de error si existe
            if (errorMessage != null) {
                ErrorMessage(
                    message = errorMessage,
                    modifier = Modifier.fillMaxWidth(formWidth)
                )
            }
            
            // Mostrar dropdown y campos solo si hay empresas cargadas (login plano OK)
            if (empresas.isNotEmpty()) {
                // Dropdown de empresas
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(formWidth)
                ) {
                    val empresaTexto = empresas.find { it.first == empresa }?.second ?: "Selecciona empresa"
                    
                    OutlinedTextField(
                        value = empresaTexto,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa", color = Color.White) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        empresas.forEach { (id, nombre) ->
                            DropdownMenuItem(
                                text = { Text(nombre) },
                                onClick = {
                                    empresa = id
                                    onEmpresaSeleccionada(id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Campo de texto para Depósito
                OutlinedTextField(
                    value = deposito,
                    onValueChange = { onDepositoChange(it) },
                    label = { Text("Depósito", color = Color.White) },
                    placeholder = { Text("Ingrese el código de depósito", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(formWidth),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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
                        onCheckedChange = { recordar = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recordar", fontSize = bodyFontSize, color = Color.White)
                }
            }
        } // Cierre del Column
        
        // Botón Identificar en posición fija (cuando no hay empresas)
        if (empresas.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        android.util.Log.d("LoginScreen", "Identificar button clicked")
                        onLogin(usuario, contrasena, recordar, empresa)
                    },
                    enabled = !isLoading && usuario.isNotBlank(),
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
        }
        
        // Botón Ingresar en posición fija (cuando hay empresas)
        if (empresas.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        android.util.Log.d("LoginScreen", "Ingresar button clicked")

                        if (prefs != null) {
                            if (recordar) {
                                // Guardar TODO si recordar está activado (incluyendo credenciales)
                                prefs.edit()
                                    .putString("savedUser", usuario)
                                    .putString("savedPass", contrasena)
                                    .putString("savedEmpresa", empresaSeleccionada)
                                    .putString("savedDeposito", deposito)
                                    .putBoolean("savedRemember", true)
                                    .apply()
                                android.util.Log.d("LoginScreen", "Guardadas credenciales completas para auto-login")
                            } else {
                                // Limpiar TODAS las credenciales guardadas
                                prefs.edit()
                                    .remove("savedUser")
                                    .remove("savedPass")
                                    .remove("savedEmpresa")
                                    .remove("savedDeposito")
                                    .putBoolean("savedRemember", false)
                                    .apply()
                                android.util.Log.d("LoginScreen", "Limpiadas todas las credenciales")
                            }
                        }
                        onLoginSuccess()
                    },
                    enabled = empresa.isNotBlank() && deposito.isNotBlank(),
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text("Ingresar", color = Color.Black, fontSize = bodyFontSize)
                }
            }
        }
    }
}
