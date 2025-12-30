package be.endevops

import be.endevops.svc.MigrationService
import be.endevops.svc.ParticipantService
import be.endevops.svc.PublisherService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.intellij.lang.annotations.Language

fun Application.configureDebugUi() {
    routing {
        get("/") {
            val publisherService: PublisherService by dependencies
            val participantService: ParticipantService by dependencies
            val migrationService: MigrationService by dependencies

            val publishers = publisherService.listAll()
            val participants = participantService.listAll()
            val migrations = migrationService.listAll()
            @Language("css")
            val style = """:root {
    color-scheme: light dark;
}

@media (prefers-color-scheme: dark) {
    :root {
        color-scheme: dark light;
    }
}

body {
    font-family: sans-serif;
    padding: 24px;
    background: light-dark(#f4f6f8, #1e1e1e);
    color: light-dark(#222, #ddd);
}

.content {
    max-width: 1100px;
    margin: 0 auto
}

h1 {
    margin-bottom: 12px
}

h2 {
    margin: 20px 0 8px 0;
    font-size: 1.1rem
}

table {
    border-collapse: collapse;
    width: 100%;
    margin-bottom: 1.25rem;
    background: light-dark(#fff, #2b2b2b);
}

th, td {
    border: 1px solid light-dark(#e6edf3, #3c3c3c);
    padding: 10px;
    text-align: left;
    vertical-align: top;
    font-size: 13px
}

th {
    background: light-dark(#f1f5f9, #313131);
    font-weight: 600
}

tbody tr:nth-child(odd) {
    background: light-dark(#fcfdff, #262626);
}

code {
    font-family: monospace;
    font-size: 12px;
    background: light-dark(#f8fafc, #2d2d2d);
    padding: 2px 4px;
    border-radius: 4px
}

.table-wrap {
    overflow-x: auto;
    border-radius: 6px
}"""

            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title { +"SML Debug UI" }
                    style {
                        +style
                    }
                }
                body {
                    div("content") {
                        h1 { +"SML Debug - Database Contents" }

                        h2 { +"Publishers" }
                        div("table-wrap") {
                            table {
                                thead {
                                    tr {
                                        th { +"Publisher ID" }
                                        th { +"Logical Address" }
                                        th { +"Physical Address" }
                                    }
                                }
                                tbody {
                                    for (p in publishers) {
                                        tr {
                                            td { +p.publisherId }
                                            td { code { +p.logicalAddress } }
                                            td { code { +p.physicalAddress } }
                                        }
                                    }
                                }
                            }
                        }

                        h2 { +"Participants" }
                        div("table-wrap") {
                            table {
                                thead {
                                    tr {
                                        th { +"Publisher ID" }
                                        th { +"Scheme" }
                                        th { +"Identifier" }
                                    }
                                }
                                tbody {
                                    for (p in participants) {
                                        tr {
                                            td { +p.publisherId }
                                            td { +p.scheme }
                                            td { code { +p.identifier } }
                                        }
                                    }
                                }
                            }
                        }

                        h2 { +"Migrations" }
                        div("table-wrap") {
                            table {
                                thead {
                                    tr {
                                        th { +"Key" }
                                        th { +"From Publisher" }
                                        th { +"To Publisher" }
                                        th { +"Scheme" }
                                        th { +"Identifier" }
                                    }
                                }
                                tbody {
                                    for (m in migrations) {
                                        tr {
                                            td { +m.key }
                                            td { +m.fromPublisher }
                                            td { +m.scheme }
                                            td { code { +m.identifier } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
