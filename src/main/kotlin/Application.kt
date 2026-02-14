package be.endevops

import be.endevops.svc.DnsClient
import be.endevops.svc.DnsConfiguration
import be.endevops.svc.MigrationService
import be.endevops.svc.ParticipantService
import be.endevops.svc.PublisherService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.di.dependencies
import org.jetbrains.exposed.sql.Database
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain
        .main(args)

fun Application.configureApplication() {
    configureMonitoring()
    install(AutoHeadResponse)
    configureBusinessIdentifier()
    configureManageServiceMetadata()
    configureDebugUi()
}

fun Application.module() {
    configureApplication()

    val dbFile = environment.config.property("dbfile").getString()
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
