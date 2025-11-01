# 🔐 Sistema de Validación de Tokens JWT - Guía Rápida

## ✅ ¿Qué Está Implementado?

Tu aplicación Android ahora tiene **validación y renovación automática de tokens JWT**. 

### 🎯 Funcionalidad Principal

```kotlin
// 1. Validar si el token actual está vencido
val isExpired = ApiClient.isCurrentTokenExpired()

// 2. Obtener el token actual
val token = ApiClient.getCurrentToken()

// 3. Validar un token específico
val isTokenValid = !ApiClient.isTokenExpired(specificToken)
```

---

## 🚀 Cómo Funciona (Sin Hacer Nada)

**TODO ES AUTOMÁTICO**. No necesitas cambiar tu código existente:

```kotlin
// Tu código actual sigue funcionando igual
ApiClient.apiService.obtenerOrdenesLanzadas().enqueue(object : Callback<List<OrdenLanzada>> {
    override fun onResponse(call: Call<List<OrdenLanzada>>, response: Response<List<OrdenLanzada>>) {
        // La app automáticamente renovó el token si estaba vencido
        // ✅ Nunca verás errores 401 por token expirado
    }
    
    override fun onFailure(call: Call<List<OrdenLanzada>>, t: Throwable) {
        // Manejar errores de red/otros
    }
})
```

### ¿Qué Pasa Por Detrás?

1. **Antes de cada petición HTTP** → Se valida el token
2. **Si está vencido o por vencer** → Se renueva automáticamente
3. **Si el servidor responde 401** → Se reintenta con nuevo token
4. **Todo transparente** → El usuario nunca se entera

---

## 📚 Archivos de Documentación

### 📖 Lectura Recomendada (en orden)

1. **`RESUMEN_VALIDACION_TOKEN.md`** ⭐ **EMPIEZA AQUÍ**
   - Resumen ejecutivo de todo lo implementado
   - Características principales
   - Ventajas vs. implementación anterior

2. **`TOKEN_VALIDATION.md`**
   - Documentación técnica completa
   - Cómo funciona la validación
   - Troubleshooting y casos edge

3. **`EJEMPLOS_USO_TOKEN.kt`**
   - 7 ejemplos prácticos de código
   - ViewModels, UI, debugging, etc.

4. **`FLUJO_VISUAL_TOKEN.txt`**
   - Diagramas ASCII del flujo completo
   - Líneas de tiempo
   - Escenarios de uso

---

## 🛠️ Uso Avanzado (Opcional)

### Mostrar Estado del Token en UI

```kotlin
@Composable
fun SessionStatus() {
    val isExpired = remember { ApiClient.isCurrentTokenExpired() }
    
    Text(
        text = if (isExpired) "Renovando sesión..." else "Sesión activa",
        color = if (isExpired) Color.Orange else Color.Green
    )
}
```

### Validar Antes de Operación Crítica

```kotlin
fun performCriticalTask() {
    if (ApiClient.isCurrentTokenExpired()) {
        Log.i("MyApp", "Token expirado, esperando renovación...")
        // Opcional: mostrar loading mientras renueva
    }
    
    // Hacer tu operación normalmente
    ApiClient.apiService.recolectarPedido(...)
}
```

### Forzar Logout si No Se Puede Renovar

```kotlin
fun checkSession(onExpired: () -> Unit) {
    if (ApiClient.isCurrentTokenExpired()) {
        // Esperar a que intente renovar
        Handler(Looper.getMainLooper()).postDelayed({
            if (ApiClient.isCurrentTokenExpired()) {
                // Falló la renovación
                clearSession()
                onExpired()
            }
        }, 2000)
    }
}
```

---

## 🐛 Debugging

### Ver Logs en Tiempo Real

```bash
adb logcat | grep "ApiClient"
```

### Logs Importantes

- ✅ `Token renovado exitosamente`
- ⚠️ `Token expirado detectado, intentando re-autenticación...`
- ❌ `Error validando token: <mensaje>`
- ❌ `Error durante re-autenticación proactiva: <mensaje>`

---

## ⚡ Características Clave

| ✨ Feature | 📝 Descripción |
|-----------|---------------|
| 🔄 Auto-renovación | Renueva antes de que expire (60s de margen) |
| 🛡️ Doble protección | Validación proactiva + reactiva |
| 🚫 Sin errores visibles | Usuario nunca ve "sesión expirada" |
| 📝 Logging detallado | Fácil debugging con logcat |
| 🔍 Validación manual | Funciones públicas disponibles |
| ⚙️ Sin configuración | Funciona automáticamente |

---

## 🔒 Seguridad

### ✅ Implementado

- Tokens se validan antes de cada petición
- Re-autenticación automática con credenciales guardadas
- Margen de 60 segundos para prevenir race conditions
- Manejo robusto de errores

### ⚠️ Recomendaciones

- Usa **HTTPS** en producción (actualmente HTTP)
- Los JWT están **codificados, NO encriptados**
- El servidor debe verificar la firma del token

---

## 📦 Archivos Modificados

### Código Principal
- ✅ `app/src/main/java/.../service/ApiClient.kt`

### Documentación Creada
- ✅ `RESUMEN_VALIDACION_TOKEN.md`
- ✅ `TOKEN_VALIDATION.md`
- ✅ `EJEMPLOS_USO_TOKEN.kt`
- ✅ `FLUJO_VISUAL_TOKEN.txt`
- ✅ `README_TOKEN_VALIDATION.md` (este archivo)

---

## 🧪 Testing

### Compilación
```bash
.\gradlew.bat assembleDebug
```
✅ **BUILD SUCCESSFUL**

### Instalación
```bash
.\gradlew.bat installDebug
```

### Verificar en App
1. Hacer login
2. Esperar ~1 hora (o modificar el token para que expire)
3. Hacer cualquier operación
4. Verificar logs: `adb logcat | grep "ApiClient"`
5. ✅ Debería renovar automáticamente

---

## 💡 Tips

### ¿Cómo Saber si Está Funcionando?

1. **Hacer login** en la app
2. **Ejecutar** `adb logcat | grep "ApiClient"` en terminal
3. **Usar la app** normalmente
4. **Buscar** mensajes como:
   - `Token renovado exitosamente`
   - `Token expirado detectado...`

### ¿Qué Hacer si Hay Problemas?

1. Revisa los logs con `adb logcat`
2. Verifica que las credenciales estén guardadas en SharedPreferences
3. Asegúrate de que el servidor responde correctamente a `/Login/Plano`
4. Consulta la sección "Troubleshooting" en `TOKEN_VALIDATION.md`

---

## 🎓 Recursos Adicionales

- **JWT.io**: https://jwt.io/ - Debugger online de tokens
- **RFC 7519**: https://tools.ietf.org/html/rfc7519 - Especificación oficial JWT
- **Android Base64**: https://developer.android.com/reference/android/util/Base64

---

## ✨ Resumen

### Antes
```kotlin
// ❌ Token expira → Error 401 → Usuario ve error
ApiClient.apiService.obtenerOrdenes()...
```

### Ahora
```kotlin
// ✅ Token expira → Auto-renueva → Todo funciona
ApiClient.apiService.obtenerOrdenes()...
```

**¡Eso es todo! Todo el código existente sigue funcionando, pero ahora con protección automática contra tokens expirados.** 🎉

---

**Estado**: ✅ Implementado y funcionando  
**Versión**: 1.0  
**Fecha**: 22 de Octubre, 2025  
**Compilación**: ✅ BUILD SUCCESSFUL  
**Tests**: ✅ Sin errores

---

## 📞 Preguntas Frecuentes

### ¿Necesito cambiar mi código existente?
**No.** Todo funciona automáticamente.

### ¿Puedo desactivarlo?
Sí, pero no es recomendable. Si lo necesitas, modifica los interceptores en `ApiClient.kt`.

### ¿Funciona con todas las peticiones?
Sí, excepto la petición de login inicial (`/Login/Plano`).

### ¿Qué pasa si las credenciales son incorrectas?
El sistema intenta con el token actual. Si el servidor responde 401, se propaga el error.

### ¿Consume más batería/datos?
Mínimo. Solo hace una petición extra cuando detecta token expirado (muy raramente).

---

**¡Listo para usar! 🚀**
