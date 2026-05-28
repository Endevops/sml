package be.endevops

import be.endevops.svc.DnsClient
import be.endevops.svc.DnsConfiguration
import be.endevops.svc.MigrationService
import be.endevops.svc.ParticipantService
import be.endevops.svc.PublisherService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.Configuration
import io.ktor.serialization.kotlinx.serialization
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondTextWriter
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.PrintWriter
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain
        .main(args)

/**
 * Registers the `application/xml` (or another specified [contentType]) content type
 * to the [ContentNegotiation] plugin using kotlinx.serialization.
 *
 * You can learn more from the corresponding [client](https://ktor.io/docs/client-serialization.html#register_xml) and [server](https://ktor.io/docs/server-serialization.html#register_xml) documentation.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.serialization.kotlinx.xml.xml)
 *
 * @param format instance.
 * @param contentType to register with, `application/xml` by default
 */
fun Configuration.xml_10(
    format: XML = XML.fast_1_0(),
    contentType: ContentType = ContentType.Application.Xml,
) {
    serialization(contentType, format)
}

@OptIn(ExperimentalXmlUtilApi::class)
val APPLICATION_XML =
    XML.v1 {
        this.xmlDeclMode = XmlDeclMode.Auto
        this.xmlVersion = XmlVersion.XML10
        setIndent(0)

        this.policy {
            this.setDefaults_1_0_0()
            this.ignoreUnknownChildren()
            this.verifyElementOrder = true
            this.isInlineCollapsedDefault = false
            this.isStrictAttributeNames = true
        }
    }

fun Application.configureApplication() {
    install(ContentNegotiation) {
        xml_10(format = APPLICATION_XML)
        xml_10(format = APPLICATION_XML, ContentType.Text.Xml)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application
                .log
                .error("Unhandled exception:", cause)
            call.respondTextWriter(status = HttpStatusCode.InternalServerError) { PrintWriter(this).use { cause.printStackTrace(it) } }
        }
    }
    configureMonitoring()
    install(AutoHeadResponse)
    configureBusinessIdentifier()
    configureManageServiceMetadata()
    configureDebugUi()
}

fun Application.module() {
    configureApplication()

    val dbFile =
        environment.config
            .property("dbfile")
            .getString()
    log.debug("Database url: {}", dbFile)
    dependencies.provide {
        Database.connect("jdbc:sqlite:$dbFile", "org.sqlite.JDBC")
    }

    configureServices()
}

fun Application.testModule() {
    configureApplication()

    val temp = createTempFile("testdb", ".db")
    temp.deleteExisting()

    dependencies.provide {
        Database.connect("jdbc:sqlite:$temp", "org.sqlite.JDBC")
    }

    configureServices()
}

fun Application.configureServices() {
    dependencies {
        provide(DnsConfiguration::class)
        provide(PublisherService::class)
        provide(DnsClient::class)
        provide(MigrationService::class)
        provide(ParticipantService::class)
        provide(ManageBusinessIdentifier::class)
        provide(ManageServiceMetadata::class)
    }
}
