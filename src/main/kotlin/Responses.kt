import com.google.gson.annotations.SerializedName

data class Tokens(val access_token: String)

data class Metadata(@SerializedName(value = "guid", alternate = ["id"]) val guid: String)
data class Entity(val name: String?, val description: String, val state: String?)
data class Resource(val metadata: Metadata, val entity: Entity)
data class Resources(val resources: List<Resource>)