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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
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
    android.util.Log.d("LoginScreen", "LoginScreen composable is being rendered")
    var usuario by remember { mutableStateOf(savedUser) }
    var contrasena by remember { mutableStateOf(savedPass) }
    var recordar by remember { mutableStateOf(savedRemember) }
    var empresa by remember { mutableStateOf(savedEmpresa) }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            android.util.Log.d("LoginScreen", "Rendering login form")
            Text("Iniciar Sesión", fontSize = 28.sp, color = Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(

                //placeholder = { Text(usuario, color= Color.Black) },
                value = usuario,
                onValueChange = {
                    usuario = it
                    android.util.Log.d("LoginScreen", "User input updated: $usuario")
                },
                label = { Text("Usuario",  color= Color.Gray) },
                placeholder = { Text("Ingrese su usuario", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                textStyle = LocalTextStyle.current.copy(
                    color = if (usuario.isBlank()) Color.Gray else Color.Black
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    cursorColor = Color.Black,
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFF1976D2),
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
               // placeholder = { Text(contrasena, color= Color.Black) },
                value = contrasena,
                onValueChange = {
                    contrasena = it
                    android.util.Log.d("LoginScreen", "Password input updated")
                },
                label = { Text("Contraseña",  color= Color.Gray) },
                placeholder = { Text("Ingrese su contraseña", color = Color.Gray) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.8f),
                textStyle = LocalTextStyle.current.copy(
                    color = if (contrasena.isBlank()) Color.Gray else Color.Black
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    cursorColor = Color.Black,
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color(0xFF1976D2),
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mostrar mensaje de error si existe
            if (errorMessage != null) {
                ErrorMessage(
                    message = errorMessage,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Mostrar botón Identificar solo si no hay empresas cargadas
            if (empresas.isEmpty()) {
                Button(
                    onClick = {
                        android.util.Log.d("LoginScreen", "Identificar button clicked")
                        onLogin(usuario, contrasena, recordar, empresa)
                    },
                    enabled = !isLoading && usuario.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Identificar", color = Color.White)
                    }
                }
            }
            // Mostrar dropdown y botón Ingresar solo si hay empresas cargadas (login plano OK)
            if (empresas.isNotEmpty()) {
                // Dropdown de empresas
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    val empresaTexto = empresas.find { it.first == empresa }?.second ?: "Selecciona empresa"
                    val esSeleccionPorDefecto = empresaTexto == "Selecciona empresa"
                    
                    OutlinedTextField(
                        value = empresaTexto,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = if (esSeleccionPorDefecto) Color.Gray else Color.Black
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.Black,
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black
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
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo de texto para Depósito
                OutlinedTextField(
                    value = deposito,
                    onValueChange = { onDepositoChange(it) },
                    label = { Text("Depósito", color = Color.Gray) },
                    placeholder = { Text("Ingrese el código de depósito", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    textStyle = LocalTextStyle.current.copy(
                        color = if (deposito.isBlank()) Color.Gray else Color.Black
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.Black,
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Checkbox Recordar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Checkbox(
                        checked = recordar,
                        onCheckedChange = { recordar = it },

                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recordar", fontSize = 16.sp, color= Color.Black)
                }
                Button(
                    onClick = {
                        android.util.Log.d("LoginScreen", "Ingresar button clicked")

                        if (prefs != null) {
                            if (recordar) {
                                // Guardar todo si recordar está activado
                                prefs.edit()
                                    .putString("savedEmpresa", empresaSeleccionada)
                                    .putString("savedDeposito", deposito)
                                    .putBoolean("savedRemember", recordar)
                                    .apply()
                            } else {
                                // Solo guardar recordar = false, limpiar el resto
                                prefs.edit()
                                    .remove("savedEmpresa")
                                    .remove("savedDeposito")
                                    .putBoolean("savedRemember", recordar)
                                    .apply()
                            }
                        }
                        onLoginSuccess()
                    },
                    enabled = empresa.isNotBlank() && deposito.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Ingresar", color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "LoginScreen Preview")
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLoginSuccess = {},
        isLoading = false,
        errorMessage = null,
        empresas = listOf("1" to "Empresa Uno", "2" to "Empresa Dos"),
        empresaSeleccionada = "1",
        onEmpresaSeleccionada = {},
        deposito = "DEP01",
        onDepositoChange = {},
        onLogin = { _, _, _, _ -> },
        savedUser = "usuario",
        savedPass = "",
        savedRemember = false,
        savedEmpresa = "1",
        prefs = null
    )
}