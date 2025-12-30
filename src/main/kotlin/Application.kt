package be.endevops

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
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
        val db = Database.connect("jdbc:sqlite:$dbFile", "org.sqlite.JDBC")
        LoggerFactory.getLogger(ParticipantService::class.java)
        db
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
        val db = Database.connect("jdbc:sqlite:$temp", "org.sqlite.JDBC")
        LoggerFactory.getLogger(ParticipantService::class.java)
        db
    }

    dependencies.provide {
        environment.config.property("dns").getAs<DnsConfiguration>()
    }
    configureServices()
}

fun Application.configureServices() {
    dependencies {
        provide(PublisherService::class)
        provide(DnsClient::class)
        provide(ParticipantService::class)
    }
}
