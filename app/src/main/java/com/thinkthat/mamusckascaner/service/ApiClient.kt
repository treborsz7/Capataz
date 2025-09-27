package com.thinkthat.mamusckascaner.service.Services

import android.content.Context
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

data class UbicacionResponse(
    val numero: Number,
    val nombre: String,
    val alias: String,
    var orden: Number,
    val codArticulo: String,
    val cantidad: Number
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
        @Header("generaRemitoDePic") generaRemitoDePic: Boolean = false

    ): retrofit2.Call<okhttp3.ResponseBody>

    @GET("UB082/UbicacionesParaRecolectar")
    fun UbicacionesParaRecolectar(
        @Header("idPed") idPed: Int,
        @Header("optimizaRecorrido") optimizaRecorrido: Boolean = false,
        
    ): retrofit2.Call<List<UbicacionResponse>>

    // Alternate path-style endpoint to handle servers expecting IdOT in the URL path
    @GET("PP082/UbicacionesParaRecolectar")
    fun UbicacionesParaRecolectarByPath(
        @Header("idOT") idOT: Int,
        @Header("optimizaRecorrido") optimizaRecorrido: Boolean = false
    ): retrofit2.Call<List<UbicacionResponse>>

    // Alternate query-style endpoint to handle servers expecting IdOT as a query parameter

    // New nested path variant: PP090/{idOT}/UbicacionesParaRecolectar

}

// --- SINGLETON DE RETROFIT ---

object ApiClient {
    private const val BASE_URL = "http://191.235.41.83:18001/"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        // No agregar token ni idEmp si es loginPlano
        if (originalRequest.url.encodedPath.endsWith("/Login/Plano")) {
            return@Interceptor chain.proceed(originalRequest)
        }
        val prefs = context?.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
        val token = prefs?.getString("token", null)
        val idEmp = prefs?.getString("savedEmpresa", null)
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