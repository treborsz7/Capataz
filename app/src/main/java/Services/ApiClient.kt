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
    @FormUrlEncoded
    @POST("/login/Plano")
    suspend fun loginPlano(
        @Field("usuario") usuario: String,
        @Field("contraseña") contraseña: String
    ): Response<LoginPlanoResponse>

    @GET("/loginEmpresa/{idEmpresa}")
    suspend fun loginEmpresa(
        @Path("idEmpresa") idEmpresa: String
    ): Response<LoginEmpresaResponse>

    @POST("/stakUbi/estivar")
    suspend fun estivar(
        @Body body: EstivarRequest
    ): Response<EstivarResponse>
}

// --- SINGLETON DE RETROFIT ---

object ApiClient {
    private const val BASE_URL = "http://server:18001/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}