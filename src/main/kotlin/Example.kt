import io.ktor.application.*
import io.ktor.content.*
import io.ktor.html.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.html.*

@Location("flows") data class Flows(val project: String)
@Location("metrics") data class Metrics(val project: String, val flow: String)

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
    install(Locations)
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
                    form("flows", method = FormMethod.get) {
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
        get<Flows> {
            val project = it.project
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
                                    if (state == "running")
                                        a(locations.href(Metrics(project, flow.metadata.guid))) {
                                            + state.capitalize()
                                        }
                                    else
                                        + state.capitalize()
                                }
                            }
                    }
                }
            }
        }
        get<Metrics> {
            val metrics = getMetrics(call.sessions.get<SessionData>()!!.bearer, it.project, it.flow)
            call.respondHtml {
                template {
                    h2 {
                        +"Metrics"
                    }
                    table {
                        tr {
                            th {
                                + "Source"
                            }
                            th {
                                + "Target"
                            }
                            th {
                                + "Tuples Submitted"
                            }
                            th {
                                + "Tuples Processed"
                            }
                            th {
                                + "Tuples Dropped"
                            }
                            th {
                                + "Exceptions Caught"
                            }
                        }
                        for ((source, targets) in metrics)
                            for ((target, metric) in targets)
                                tr {
                                    td {
                                        + source
                                    }
                                    td {
                                        + target
                                    }
                                    td {
                                        + metric.n_tuples_submitted.toString()
                                    }
                                    td {
                                        + metric.n_tuples_processed.toString()
                                    }
                                    td {
                                        + metric.n_tuples_dropped.toString()
                                    }
                                    td {
                                        + metric.n_exceptions_caught.toString()
                                    }
                                }
                    }
                }
            }
        }
    }
}