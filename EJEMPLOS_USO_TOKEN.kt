// Ejemplo de uso de la validación de tokens en tu aplicación

import com.thinkthat.mamusckascaner.service.ApiClient

// ========================================
// EJEMPLO 1: Validar token antes de una operación crítica
// ========================================

fun performCriticalOperation() {
    // Verificar si el token está vencido antes de hacer algo importante
    if (ApiClient.isCurrentTokenExpired()) {
        Log.w("MyActivity", "⚠️ Token expirado, el sistema renovará automáticamente")
        // No necesitas hacer nada, el interceptor lo manejará automáticamente
        // Pero puedes mostrar un mensaje al usuario si quieres
    }
    
    // Hacer tu operación normalmente
    ApiClient.apiService.obtenerOrdenesLanzadas().enqueue(object : Callback<List<OrdenLanzada>> {
        override fun onResponse(call: Call<List<OrdenLanzada>>, response: Response<List<OrdenLanzada>>) {
            if (response.isSuccessful) {
                // Procesar respuesta
            }
        }
        override fun onFailure(call: Call<List<OrdenLanzada>>, t: Throwable) {
            Log.e("MyActivity", "Error: ${t.message}")
        }
    })
}

// ========================================
// EJEMPLO 2: Mostrar estado del token en UI
// ========================================

@Composable
fun TokenStatusIndicator() {
    val isExpired = remember { ApiClient.isCurrentTokenExpired() }
    val token = remember { ApiClient.getCurrentToken() }
    
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isExpired) Color.Red else Color.Green
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isExpired) "Token expirado" else "Sesión activa",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ========================================
// EJEMPLO 3: Forzar logout si el token no se puede renovar
// ========================================

fun checkSessionValidity(context: Context, onSessionExpired: () -> Unit) {
    if (ApiClient.isCurrentTokenExpired()) {
        Log.w("SessionCheck", "Token expirado detectado")
        
        // Esperar un momento para que el interceptor intente renovar
        Handler(Looper.getMainLooper()).postDelayed({
            // Verificar de nuevo después de dar tiempo a la renovación
            if (ApiClient.isCurrentTokenExpired()) {
                Log.e("SessionCheck", "No se pudo renovar el token, cerrando sesión")
                
                // Limpiar datos de sesión
                val prefs = context.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    remove("token")
                    remove("savedUser")
                    remove("savedPass")
                    remove("savedEmpresa")
                    apply()
                }
                
                // Notificar a la UI
                onSessionExpired()
            } else {
                Log.i("SessionCheck", "✅ Token renovado exitosamente")
            }
        }, 2000) // Esperar 2 segundos
    }
}

// ========================================
// EJEMPLO 4: Usar en un ViewModel con StateFlow
// ========================================

class AuthViewModel : ViewModel() {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Valid)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    init {
        // Verificar estado de sesión cada 5 minutos
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 minutos
                checkTokenStatus()
            }
        }
    }
    
    private fun checkTokenStatus() {
        when {
            ApiClient.getCurrentToken() == null -> {
                _sessionState.value = SessionState.NotAuthenticated
            }
            ApiClient.isCurrentTokenExpired() -> {
                _sessionState.value = SessionState.Expired
            }
            else -> {
                _sessionState.value = SessionState.Valid
            }
        }
    }
    
    sealed class SessionState {
        object Valid : SessionState()
        object Expired : SessionState()
        object NotAuthenticated : SessionState()
    }
}

// ========================================
// EJEMPLO 5: Interceptor personalizado para logging
// ========================================

class TokenLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = ApiClient.getCurrentToken()
        
        if (token != null) {
            val isExpired = ApiClient.isTokenExpired(token)
            Log.d("TokenLogger", "📋 Petición: ${chain.request().url}")
            Log.d("TokenLogger", "🔑 Token ${if (isExpired) "EXPIRADO" else "VÁLIDO"}")
            
            if (isExpired) {
                Log.w("TokenLogger", "⚠️ Enviando petición con token expirado (se renovará automáticamente)")
            }
        }
        
        return chain.proceed(chain.request())
    }
}

// ========================================
// EJEMPLO 6: Decodificar y mostrar información del token
// ========================================

fun decodeAndShowTokenInfo(token: String): String {
    return try {
        val parts = token.split(".")
        if (parts.size != 3) return "Token inválido"
        
        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
        val decodedString = String(decodedBytes, Charsets.UTF_8)
        val jsonPayload = JSONObject(decodedString)
        
        buildString {
            appendLine("📄 Información del Token:")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            
            if (jsonPayload.has("sub")) {
                appendLine("👤 Usuario: ${jsonPayload.getString("sub")}")
            }
            
            if (jsonPayload.has("exp")) {
                val exp = jsonPayload.getLong("exp")
                val date = Date(exp * 1000)
                val isExpired = ApiClient.isTokenExpired(token)
                appendLine("⏰ Expira: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)}")
                appendLine("📊 Estado: ${if (isExpired) "❌ EXPIRADO" else "✅ VÁLIDO"}")
            }
            
            if (jsonPayload.has("iat")) {
                val iat = jsonPayload.getLong("iat")
                val date = Date(iat * 1000)
                appendLine("📅 Emitido: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)}")
            }
            
            if (jsonPayload.has("idEmp")) {
                appendLine("🏢 Empresa: ${jsonPayload.getString("idEmp")}")
            }
        }
    } catch (e: Exception) {
        "❌ Error decodificando token: ${e.message}"
    }
}

// ========================================
// EJEMPLO 7: Pantalla de debug para desarrolladores
// ========================================

@Composable
fun TokenDebugScreen() {
    val token = ApiClient.getCurrentToken()
    val isExpired = ApiClient.isCurrentTokenExpired()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🔧 Token Debug",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado: ${if (isExpired) "❌ Expirado" else "✅ Válido"}")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (token != null) {
                    Text(
                        text = decodeAndShowTokenInfo(token),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text("Sin token")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                // Forzar verificación
                val status = if (ApiClient.isCurrentTokenExpired()) "expirado" else "válido"
                Log.i("TokenDebug", "Token actual: $status")
            }
        ) {
            Text("Verificar Token")
        }
    }
}
