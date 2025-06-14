import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
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
    val message: String?
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

// --- INTERFAZ DE LA API ---

interface ApiService {
    @POST("Login/Plano")
    fun loginPlano(
        @Header("nombreUsuario") nombreUsuario: String,
        @Header("pass") pass: String
    ): retrofit2.Call<okhttp3.ResponseBody>

    @POST("loginEmpresa/{idEmpresa}")
    fun loginEmpresa(
        @Path("idEmpresa") idEmpresa: String,
        @Header("Authorization") authorization: String
    ): retrofit2.Call<LoginEmpresaResponse>

    @POST("StkUbi/Estibar")
    suspend fun estivar(
        @Body body: EstivarRequest
    ): Response<EstivarResponse>
}

object TokenProvider {
    var token: String? = null
}

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val originalRequest: Request = chain.request()
        // Llamar a loginEmpresa con el token como Bearer
        val token = TokenProvider.token
        android.util.Log.d("token", "llama api con token: $token")
        return if (!token.isNullOrEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

// --- SINGLETON DE RETROFIT ---

object ApiClient {
    private const val BASE_URL = "http://191.235.41.83:18001/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
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