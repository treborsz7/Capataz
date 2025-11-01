# Validación de Token JWT

## Descripción

Se ha implementado un sistema robusto de validación y renovación automática de tokens JWT en el `ApiClient`.

## Funcionalidades Implementadas

### 1. **Validación de Expiración del Token**

```kotlin
// Validar si un token específico está vencido
val isExpired = ApiClient.isTokenExpired(token)

// Validar si el token actual (guardado en SharedPreferences) está vencido
val isCurrentExpired = ApiClient.isCurrentTokenExpired()

// Obtener el token actual
val currentToken = ApiClient.getCurrentToken()
```

### 2. **Renovación Automática Proactiva**

El `authInterceptor` ahora valida el token **antes** de cada petición:
- Si el token está vencido (o está por vencer en menos de 60 segundos), intenta re-autenticarse automáticamente
- Si la re-autenticación es exitosa, usa el nuevo token para la petición
- Todo esto ocurre de forma transparente, sin que el usuario lo note

### 3. **Renovación Reactiva en Respuesta 401**

El `reAuthInterceptor` sigue funcionando como respaldo:
- Si el servidor responde con 401 (no autorizado), intenta re-autenticarse
- Si obtiene un nuevo token, reintenta la petición original

## Cómo Funciona la Validación

Los JWT tienen tres partes separadas por puntos: `header.payload.signature`

El **payload** contiene información codificada en Base64URL, incluyendo:
- `exp`: Timestamp Unix de expiración (en segundos)
- `iat`: Timestamp de emisión
- `sub`: Subject (usuario)
- Otros claims personalizados

La función `isTokenExpired()`:
1. Divide el token en sus tres partes
2. Decodifica el payload de Base64URL
3. Parsea el JSON resultante
4. Lee el claim `exp` (expiration time)
5. Compara con la hora actual
6. Retorna `true` si el token está vencido o está por vencer en menos de 60 segundos

## Margen de Seguridad

El sistema usa un **margen de 60 segundos** antes de la expiración real para:
- Evitar condiciones de carrera (el token expira justo cuando se envía la petición)
- Dar tiempo suficiente para completar la re-autenticación
- Mejorar la experiencia del usuario (evitar errores 401 visibles)

## Ejemplo de Uso en tu Código

Si necesitas validar manualmente el token en algún ViewModel o Activity:

```kotlin
// En cualquier parte de tu código
if (ApiClient.isCurrentTokenExpired()) {
    // El token está vencido
    Log.w("MyActivity", "El token ha expirado")
    // Opcionalmente, puedes forzar un logout o mostrar mensaje
} else {
    // El token es válido
    // Proceder con operaciones normales
}
```

## Logs

El sistema genera logs informativos:
- `Token expirado detectado, intentando re-autenticación...` (nivel WARNING)
- `Token renovado exitosamente` (nivel INFO)
- Errores específicos con stack traces (nivel ERROR)

Puedes filtrar los logs con: `adb logcat | grep "ApiClient"`

## Ventajas de esta Implementación

✅ **Renovación Automática**: El usuario nunca ve errores de token expirado  
✅ **Doble Protección**: Validación proactiva + reactiva (respaldo ante 401)  
✅ **Sin Dependencias Externas**: Usa solo `android.util.Base64` y `org.json.JSONObject`  
✅ **Margen de Seguridad**: Renueva 60 segundos antes de la expiración real  
✅ **Manejo de Errores**: Si el token es inválido o está corrupto, lo detecta y re-autentica  
✅ **Logging Detallado**: Fácil de debuggear y monitorear

## Casos Edge Manejados

- Token sin campo `exp`: Se considera válido indefinidamente
- Token malformado: Se considera expirado
- Token corrupto (Base64 inválido): Se considera expirado
- Credenciales no guardadas: No intenta re-autenticar
- Error de red durante re-autenticación: Continúa con el token actual (el reAuthInterceptor lo manejará)

## Notas de Seguridad

⚠️ **Importante**: Los JWT están codificados, NO encriptados. Cualquiera puede decodificar el payload y leer su contenido. Por eso:
- No guardes información sensible en el token
- Usa siempre HTTPS en producción
- El token debe ser verificado en el servidor (la firma)

## Testing

Para probar la funcionalidad:

```kotlin
// Simular un token expirado
val expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1MTYyMzkwMjJ9.signature"
println(ApiClient.isTokenExpired(expiredToken)) // true

// Simular un token válido (expira en el año 2030)
val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE5MDY4ODk2MDB9.signature"
println(ApiClient.isTokenExpired(validToken)) // false
```

## Troubleshooting

**Problema**: El token se renueva constantemente  
**Solución**: Verifica que el servidor esté devolviendo un token con `exp` válido

**Problema**: No se renueva automáticamente  
**Solución**: Asegúrate de que:
- `savedUser` y `savedPass` están guardados en SharedPreferences
- El servidor en `${BASE_URL}Login/Plano` está respondiendo correctamente
- Revisa los logs con `adb logcat | grep "ApiClient"`

**Problema**: Error `IllegalArgumentException` al decodificar  
**Solución**: El token no es un JWT válido. Verifica que el servidor esté enviando el formato correcto.
