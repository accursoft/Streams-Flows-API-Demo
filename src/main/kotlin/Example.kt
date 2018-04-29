import io.ktor.application.*
import io.ktor.content.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.html.*

fun HTML.template(block: BODY.() -> Unit) {
    head {
        link("/static/styles.css", "stylesheet", "text/css")
        title("Streams Designer API Demo")
    }
    body {
        block()
    }
}

fun Application.main() {
    install(Sessions) {
        cookie<SessionData>("session")
    }
    install(Routing) {
        static("static") {
           resource("styles.css")
        }
        get("/") {
            call.sessions.clear<SessionData>()
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
            val bearer = getBearer(call.receiveParameters()["key"]!!)
            call.sessions.set(SessionData(bearer))
            val projects = getProjects(bearer)

            call.respondHtml {
                template {
                    form("flows", method = FormMethod.post) {
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
                                }
                            }
                        }
                        + " "
                        submitInput()
                    }
                }
            }
        }
        post("flows") {
            val project = call.receiveParameters()["project"]!!
            val bearer = call.sessions.get<SessionData>()!!.bearer

            val flows = getFlows(bearer, project).map {
                it to getState(bearer, project, it.metadata.guid)
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
                        for ((flow, state) in flows)
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