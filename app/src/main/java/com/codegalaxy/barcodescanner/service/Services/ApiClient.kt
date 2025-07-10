import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
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
    val codArticulo: String
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

    @POST("UB082/RecolectarPedido")
    fun recolectarPedido(
        //@Body body: okhttp3.RequestBody
    ): retrofit2.Call<okhttp3.ResponseBody>

    @POST("UB091/ReubicarPartidas")
    fun reubicarPartidas(
        @Body body: ReubicarPartidasRequest
        //@Body body: JSONObject

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
        // No agregar token ni idEmp si es loginPlano
        if (originalRequest.url().encodedPath().endsWith("/Login/Plano")) {
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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
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