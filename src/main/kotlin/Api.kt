import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.formUrlEncode

data class Tokens(val access_token: String)

data class Metadata(@SerializedName(value = "guid", alternate = ["id"]) val guid: String)
data class Entity(val name: String?, val description: String, val state: String?)
data class Resource(val metadata: Metadata, val entity: Entity)
data class Resources(val resources: List<Resource>)

data class SessionData(val bearer: String)

val client = HttpClient(CIO) { install(JsonFeature) }

suspend fun getBearer(key: String) =
        client.post<Tokens>("https://iam.ng.bluemix.net/oidc/token") {
            body = TextContent(
                listOf(
                        "grant_type" to "urn:ibm:params:oauth:grant-type:apikey",
                        "apikey" to key
                ).formUrlEncode(),
                ContentType.Application.FormUrlEncoded
            )
        }.access_token

suspend fun getResources(bearer: String, endpoint: String) =
        client.get<Resources>("https://api.apsportal.ibm.com/v2/$endpoint") {
            headers["Authorization"] = "Bearer $bearer"
        }.resources

suspend fun getProjects(bearer: String) =
        getResources(bearer, "projects")

suspend fun getFlows(bearer: String, project: String) =
        getResources(bearer, "streams_flows?project_id=$project")

suspend fun getState(bearer: String, project: String, flow: String) =
        getResources(bearer, "streams_flows/$flow/runs?project_id=$project").first().entity.state!!