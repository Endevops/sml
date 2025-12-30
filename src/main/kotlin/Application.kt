package be.endevops

import be.endevops.svc.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain
        .main(args)

fun Application.module() {
    configureMonitoring()
    configureRouting()
    configureBusinessIdentifier()
    configureManageServiceMetadata()

    val dbFile = environment.config.property("dbfile").getString()
    log.debug("Database url: {}", dbFile)
    dependencies.provide {
        Database.connect("jdbc:sqlite:$dbFile", "org.sqlite.JDBC")
    }

    configureServices()
}

fun Application.testModule() {
    configureMonitoring()
    configureRouting()
    configureBusinessIdentifier()
    configureManageServiceMetadata()

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
