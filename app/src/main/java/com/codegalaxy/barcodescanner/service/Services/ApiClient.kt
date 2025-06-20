import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
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
    val codArticulo: String
)

// --- INTERFAZ DE LA API ---

interface ApiService {
    @POST("Login/Plano")
    fun loginPlano(
        @Header("nombreUsuario") nombreUsuario: String,
        @Header("pass") pass: String
    ): retrofit2.Call<okhttp3.ResponseBody>

    @GET("loginEmpresa/{idEmpresa}")
     fun loginEmpresa(
        @Path("idEmp ") idEmp : String
    ): retrofit2.Call<LoginEmpresaResponse>

    @POST("UB090/EstibarPartidas")
    suspend fun estivar(
        @Body body: EstivarRequest
    ): retrofit2.Call<EstivarResponse>

    @GET("UB090/UbicacionesParaEstibar")
     fun ubicacionesParaEstibar(
        @Header ("idEmp") idEmp : Number,
        @Header ("codDeposi") codDeposi : String,
        @Header ("codArticu") codArticu : String?,
        @Header ("optimizaRecorrido") optimizaRecorrido: Boolean

    ): retrofit2.Call<okhttp3.ResponseBody>
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
        // No agregar token si es loginPlano
        if (originalRequest.url().encodedPath().endsWith("/Login/Plano")) {
            return@Interceptor chain.proceed(originalRequest)
        }
        val prefs = context?.getSharedPreferences("QRCodeScannerPrefs", Context.MODE_PRIVATE)
        val token = prefs?.getString("token", null)
        val newRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
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