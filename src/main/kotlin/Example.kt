import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.html.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.coroutines.delay
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
                        + "Streams Flows API Demo"
                    }
                    form("projects", method = FormMethod.post) {
                        label {
                            a("https://console.bluemix.net/iam/#/apikeys") {
                                + "API Key"
                            }
                            br
                            passwordInput {
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

            val flows = getFlows(bearer, project).associateWith {
                getState(bearer, project, it.metadata.guid)
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
            val metrics1 = getMetrics(call.sessions.get<SessionData>()!!.bearer, it.project, it.flow)
            delay(1000)
            val metrics2 = getMetrics(call.sessions.get<SessionData>()!!.bearer, it.project, it.flow)
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
                        for ((source, targets) in metrics1)
                            for ((target, metrics1) in targets)
                                tr {
                                    td {
                                        + source
                                    }
                                    td {
                                        + target
                                    }
                                    for (metric in listOf("n_tuples_submitted", "n_tuples_processed", "n_tuples_dropped", "n_exceptions_caught"))
                                        td {
                                            metrics1[metric]?.let { metric1 ->
                                                val metric2 = metrics2[source]!![target]!![metric]!!
                                                title = metric2.value.toString()
                                                + ((metric2.value - metric1.value).toFloat() / (metric2.lastTimeRetrieved - metric1.lastTimeRetrieved) * 1000).toInt().toString()
                                            }
                                        }
                                }
                    }
                    p {
                        +"Metrics are per second, hover for total."
                    }
                }
            }
        }
    }
}