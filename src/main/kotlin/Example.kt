import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.html.*
import io.ktor.http.ContentType
import io.ktor.http.formUrlEncode
import io.ktor.request.*
import io.ktor.routing.*
import kotlinx.html.*

fun HTML.template(block: BODY.() -> Unit) {
    head {
        link("/static/styles.css", "stylesheet", "text/css")
    }
    body {
        block()
    }
}

fun Application.main() {
    install(Routing) {
        static("static") {
           resource("styles.css")
        }
        get("/") {
            call.respondHtml {
                template {
                    h1 {
                        + "Streams Designer API Demo"
                    }
                    form("projects", method = FormMethod.post) {
                        label {
                            a("https://console.bluemix.net/iam/#/apikeys") {
                                + "API Key"
                            }
                            br
                            textInput {
                                name = "key"
                            }
                        }
                        br
                        submitInput()
                    }
                }
            }
        }
        post("projects") {
            val client = HttpClient(CIO) {
                install(JsonFeature)
            }

            val bearer = client.post<Tokens>("https://iam.ng.bluemix.net/oidc/token") {
                body = TextContent(
                            listOf(
                                "grant_type" to "urn:ibm:params:oauth:grant-type:apikey",
                                "apikey" to call.receiveParameters()["key"]
                            ).formUrlEncode(),
                            ContentType.Application.FormUrlEncoded
                )
            }.access_token
            val projects = client.get<Resources>("https://ngp-projects-api.ng.bluemix.net/v2/projects") {
                headers["Authorization"] = "Bearer $bearer"
            }.resources

            call.respondHtml {
                template {
                    form("flows", method = FormMethod.post) {
                        hiddenInput {
                            name = "bearer"
                            value = bearer
                        }
                        h2 {
                            + "Select Project"
                        }
                        select {
                            name = "project"
                            projects.map {
                                option {
                                    value = it.metadata.guid
                                    label = it.entity.name!!
                                    title = it.entity.description
                            } }
                        }
                        + " "
                        submitInput()
                    }
                }
            }
        }
        post("flows") {
            val params = call.receiveParameters()
            val bearer = params["bearer"]!!
            val project = params["project"]!!

            val client = HttpClient(CIO) {
                install(JsonFeature)
            }

            val flows = client.get<Resources>("https://streaming-pipelines-api.mybluemix.net/v2/streams_flows?project_id=$project") {
                headers["Authorization"] = "Bearer $bearer"
            }.resources.map {
                it to client.get<Resources>("https://streaming-pipelines-api.mybluemix.net/v2/streams_flows/${it.metadata.guid}/runs?project_id=$project") {
                                headers["Authorization"] = "Bearer $bearer"
                            }.resources[0].entity.state!!
            }

            call.respondHtml {
                template {
                    h2 {
                        + "Flows"
                    }
                    table {
                        tr {
                            th {
                                + "Flow"
                            }
                            th {
                                + "Status"
                            }
                        }
                        flows.map { (flow, state) ->
                            tr {
                                td {
                                    a("https://dataplatform.ibm.com/streams/pipelines/${flow.metadata.guid}?projectid=$project") {
                                        title = flow.entity.description
                                        + flow.entity.name!!
                                    }
                                }
                                td {
                                    + state.capitalize()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}