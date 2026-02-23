package com.thinkthat.mamusckascaner.service.Services

import android.content.Context
import android.util.Base64
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response

// --- MODELOS DE DATOS ---

data class LoginPlanoRequest(
    val usuario: String,
    val contraseña: String
)

data class LoginPlanoResponse(
    val token: String?,
    val success: Boolean,
    val message: String?
)

data class EmpresasObtenidaResponse(
    val idEmp: String
)



data class LoginEmpresaResponse(
    val success: Boolean,
    val message: String?,
    val id : Number,
    val nombre: String,
    val descripcion : String,
)

data class Partida(
    val numPartida: String,
    val nombreubicacion: String
)

data class EstivarRequest(
    val fechaHora: String,
    val codDeposito: String,
    val partidas: List<Partida>
)

data class EstivarResponse(
    val success: Boolean,
    val message: String?
)

data class ArticuloResponse(
    val codigo: String,
    val codigoBarras: String? = "",
    val descripcion: String,
    val id: Int? = 0,
    val llevaStock: Boolean? = false,
    val perfil: String? = "",
    val requerido: Int? = null,
    val saldoDisponible: Int? = 0,
    val sinonimo: String? = "",
    val unidadMedida: String? = "",
    val usaEscalas: String? = "",
    val usaPartidas: Boolean? = false,
    val usaSeries: Boolean? = false,
    val userData: Any? = null,
    val nroPartida: String? = null,
    // Campos adicionales del API
    val estadoVta: String? = null,
    val estaloElab: String? = null,
    val fecha: String? = null,
    val numDespacho: String? = null,
    val vencimiento: String? = null
)

data class UbicacionResponse(
    val numero: Int,
    val nombre: String,
    val alias: String,
    val orden: Int,
    val articulos: List<ArticuloResponse>? = null,
    val userData: Any? = null
)

// Wrapper para la respuesta del API que incluye "body"
data class UbicacionesWrapper(
    val body: List<UbicacionResponse>
)

data class EstibarPartida(
    val nombreUbicacion: String,
    val numPartida: String
)

data class ReubicarPartida(
    val nombreUbiOrigen: String,
    val nombreUbiDestino: String,
    val numPartida: String
)
data class EstibarPartidasRequest(
    val partidas: List<EstibarPartida>,
    val fechaHora: String,
    val codDeposito: String,
    val observacion: String? = null
)
data class ReubicarPartidasRequest(
    val reubicaciones: List<ReubicarPartida>,
    val fechaHora: String,
    val codDeposito: String,
    val observacion: String? = null,
    val reubicacion: Number
)

data class ProductoOrden(
    val codigo: String,
    val codigoBarras: String,
    val descripcion: String,
    val id: Int,
    val llevaStock: Boolean,
    val perfil: String, // "0 = Inhabilitado"
    val sinonimo: String,
    val unidadMedida: String,
    val usaEscalas: String, // "0 = No"
    val usaPartidas: Boolean,
    val usaSeries: Boolean
)

data class OrdenLanzada(
    val codEstado: String,
    val estado: String, // "0 = Generada"
    val fechaGeneracion: String, // formato ISO: "2025-07-30T02:01:35.174Z"
    val fechaLanzamiento: String, // formato ISO: "2025-07-30T02:01:35.174Z"
    val id: Int,
    val numero: String,
    val producto: ProductoOrden,
    val tipoComp: String,
    val unificadora: Boolean
)

data class OrdenTrabajoCompleta(
    val id: Int,
    val numero: String,
    val fechaGeneracion: String,
    val fechaLanzamiento: String,
    val estado: String,
    val codEstado: String,
    val tipoComp: String,
    val unificadora: Boolean,
    val producto: ProductoOrden,
    val articuloEx: List<ItemOrdenCompleto>
)

data class ItemOrdenCompleto(
    val id: Int,
    val nroPartida: String,
    val ubicacion: String,
    val cantidad: Int,
    val cantidadRecolectada: Int?,
    val estado: String
)

// --- INTERFAZ DE LA API ---

interface ApiService {
    @POST("Login/Plano")
    fun loginPlano(
        @Header("nombreUsuario") nombreUsuario: String,
        @Header("pass") pass: String
    ): retrofit2.Call<okhttp3.ResponseBody>

    @GET("Empresa/Get")
    fun EmpresasGet(): retrofit2.Call<List<LoginEmpresaResponse>>


    @POST("loginEmpresa/{idEmpresa}")
     fun loginEmpresa(
        @Path("idEmp ") idEmp : String
    ): retrofit2.Call<LoginEmpresaResponse>

    @POST("UB090/EstibarPartidas")
    suspend fun estivar(
        @Body body: EstivarRequest
    ): retrofit2.Call<EstivarResponse>

    @GET("UB090/UbicacionesParaEstibar")
     fun ubicacionesParaEstibar(
        @Header ("codDeposi") codDeposi : String,
        @Header ("codArticu") codArticu : String?,
        @Header ("optimizaRecorrido") optimizaRecorrido: Boolean

    ): retrofit2.Call<okhttp3.ResponseBody>

    @POST("UB090/EstibarPartidas")
    fun estibarPartidas(
        @Body body: EstibarPartidasRequest
    ): retrofit2.Call<okhttp3.ResponseBody>


    @POST("UB091/ReubicarPartidas")
    fun reubicarPartidas(
        @Body body: ReubicarPartidasRequest
        //@Body body: JSONObject

    ): retrofit2.Call<okhttp3.ResponseBody>

    @GET("PP090/Lanzadas")
    fun obtenerOrdenesLanzadas(): retrofit2.Call<List<OrdenLanzada>>

    @GET("PP090/{id}")
    fun obtenerOrdenById(
        @Path("id") id: Int
    ): retrofit2.Call<OrdenLanzada>

    @POST("UB082/RecolectarPedido")
    fun recolectarPedido(
        @Body body: okhttp3.RequestBody,
        @Header("avisosComoError") avisosComoError: Boolean = false,
        @Header("generaRemitoDePic") generaRemitoDePic: Boolean = true,
        @Header("RECOLECTAR_PEDIDOS.verifica_id_unico_de_etiqueta") verificaIdUnicoDeEtiqueta: String = "Flexible"

    ): retrofit2.Call<okhttp3.ResponseBody>

    @GET("UB082/UbicacionesParaRecolectar")
    fun UbicacionesParaRecolectar(
        @Header("idPed") idPed: Int,
        @Header("optimizaRecorrido") optimizaRecorrido: Boolean = false,
        
    ): retrofit2.Call<List<UbicacionResponse>>


}

// --- SINGLETON DE RETROFIT ---

object ApiClient {
    private const val BASE_URL = "http://191.235.41.83:18001/"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Valida si un token JWT está vencido.
     * @param token El token JWT a validar
     * @return true si el token está vencido o es inválido, false si aún es válido
     */
    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        
        try {
            // Un JWT tiene el formato: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) return true
            
            // Decodificar el payload (segunda parte)
            val payload = parts[1]
            
            // Decodificar de Base64URL a String
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            
            // Parsear el JSON del payload
            val jsonPayload = JSONObject(decodedString)
            
            // Obtener el claim 'exp' (expiration time en segundos Unix)
            if (!jsonPayload.has("exp")) {
                // Si no tiene campo exp, consideramos que no expira nunca
                return false
            }
            
            val exp = jsonPayload.getLong("exp")
            val currentTimeInSeconds = System.currentTimeMillis() / 1000
            
            // Agregar un margen de 60 segundos antes de que expire
            // para re-autenticar proactivamente
            return currentTimeInSeconds >= (exp - 60)
            
        } catch (e: Exception) {
            // Si hay cualquier error decodificando, asumir que está vencido
            android.util.Log.e("ApiClient", "Error validando token: ${e.message}")
            return true
        }
    }
    
    /**
     * Obtiene el token actual de SharedPreferences
     */
    fun getCurrentToken(): String? {
        val prefs = context?.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
        return prefs?.getString("token", null)
    }
    
    /**
     * Valida si el token actual está vencido
     */
    fun isCurrentTokenExpired(): Boolean {
        return isTokenExpired(getCurrentToken())
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        // No agregar token ni idEmp si es loginPlano
        if (originalRequest.url.encodedPath.endsWith("/Login/Plano")) {
            return@Interceptor chain.proceed(originalRequest)
        }
        
        val prefs = context?.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
        var token = prefs?.getString("token", null)
        val idEmp = prefs?.getString("savedEmpresa", null)
        
        // Validar si el token está vencido antes de usarlo
        if (isTokenExpired(token)) {
            android.util.Log.w("ApiClient", "Token expirado detectado, intentando re-autenticación...")
            
            val savedUser = prefs?.getString("savedUser", null)
            val savedPassword = prefs?.getString("savedPass", null)
            
            if (!savedUser.isNullOrBlank() && !savedPassword.isNullOrBlank()) {
                try {
                    // Intentar obtener un nuevo token
                    val loginRequest = Request.Builder()
                        .url("${BASE_URL}Login/Plano")
                        .post(okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), ""))
                        .addHeader("nombreUsuario", savedUser)
                        .addHeader("pass", savedPassword)
                        .build()
                    
                    val loginResponse = chain.proceed(loginRequest)
                    
                    if (loginResponse.isSuccessful) {
                        val responseBody = loginResponse.body?.string()
                        if (!responseBody.isNullOrBlank()) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val newToken = jsonResponse.optString("token")
                                
                                if (!newToken.isNullOrBlank() && !isTokenExpired(newToken)) {
                                    // Guardar el nuevo token
                                    prefs?.edit()?.putString("token", newToken)?.apply()
                                    token = newToken
                                    android.util.Log.i("ApiClient", "Token renovado exitosamente")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ApiClient", "Error parseando respuesta de login: ${e.message}")
                            }
                        }
                    }
                    loginResponse.close()
                } catch (e: Exception) {
                    android.util.Log.e("ApiClient", "Error durante re-autenticación proactiva: ${e.message}")
                }
            }
        }
        
        val builder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        if (!idEmp.isNullOrBlank()) {
            builder.addHeader("idEmp", idEmp)
        }
        val newRequest = builder.build()
        chain.proceed(newRequest)
    }

    private val reAuthInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        val response = chain.proceed(originalRequest)
        
        // Si recibimos 401 (no autorizado) y no es una petición de login
        if (response.code == 401 && !originalRequest.url.encodedPath.endsWith("/Login/Plano")) {
            response.close() // Cerrar la respuesta original
            
            val prefs = context?.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
            val savedUser = prefs?.getString("savedUser", null)
            val savedPassword = prefs?.getString("savedPass", null)
            
            if (!savedUser.isNullOrBlank() && !savedPassword.isNullOrBlank()) {
                // Intentar re-autenticación
                try {
                    val loginRequest = originalRequest.newBuilder()
                        .url("${BASE_URL}Login/Plano")
                        .post(okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), ""))
                        .removeHeader("Authorization")
                        .removeHeader("idEmp")
                        .addHeader("nombreUsuario", savedUser)
                        .addHeader("pass", savedPassword)
                        .build()
                    
                    val loginResponse = chain.proceed(loginRequest)
                    
                    if (loginResponse.isSuccessful) {
                        val responseBody = loginResponse.body?.string()
                        if (!responseBody.isNullOrBlank()) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val newToken = jsonResponse.optString("token")
                                
                                if (!newToken.isNullOrBlank()) {
                                    // Guardar el nuevo token
                                    prefs?.edit()?.putString("token", newToken)?.apply()
                                    
                                    // Reintentar la petición original con el nuevo token
                                    val retryRequest = originalRequest.newBuilder()
                                        .removeHeader("Authorization")
                                        .addHeader("Authorization", "Bearer $newToken")
                                        .build()
                                    
                                    loginResponse.close()
                                    return@Interceptor chain.proceed(retryRequest)
                                }
                            } catch (e: Exception) {
                                // Error parseando la respuesta del login
                            }
                        }
                    }
                    loginResponse.close()
                } catch (e: Exception) {
                    // Error durante la re-autenticación
                }
            }
            
            // Si llegamos aquí, la re-autenticación falló, devolver la respuesta 401 original
            return@Interceptor chain.proceed(originalRequest)
        }
        
        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(reAuthInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                
                // Log de la respuesta para debugging
                if (request.url.encodedPath.contains("UbicacionesParaRecolectar")) {
                    val responseBody = response.body
                    val source = responseBody?.source()
                    source?.request(Long.MAX_VALUE)
                    val buffer = source?.buffer
                    val responseBodyString = buffer?.clone()?.readUtf8()
                    
                    Log.d("API_RESPONSE", "=== RESPUESTA CRUDA ===")
                    Log.d("API_RESPONSE", "URL: ${request.url}")
                    Log.d("API_RESPONSE", "Body: $responseBodyString")
                    Log.d("API_RESPONSE", "=== FIN RESPUESTA ===")
                }
                
                response
            }
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}