package be.endevops.svc

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class MigrationService(private val database: Database) {
    object Migrations : Table() {
        val key = varchar("key", 200)
        val fromPublisher = varchar("from_publisher", 200)
        val toPublisher = varchar("to_publisher", 200)
        val scheme = varchar("scheme", 100)
        val identifier = varchar("identifier", 200)

        override val primaryKey = PrimaryKey(key, fromPublisher, toPublisher)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Migrations)
        }
    }

    data class MigrationRecord(
        val key: String,
        val fromPublisher: String,
        val toPublisher: String,
        val scheme: String,
        val identifier: String,
    )

    fun create(r: MigrationRecord) = transaction(database) {
        Migrations.insert {
            it[key] = r.key
            it[fromPublisher] = r.fromPublisher
            it[toPublisher] = r.toPublisher
            it[scheme] = r.scheme
            it[identifier] = r.identifier
        }[Migrations.key]
    }

    fun get(key: String) = transaction(database) {
        Migrations.selectAll().where {
            Migrations.key eq key
        }.singleOrNull()?.let {
            MigrationRecord(
                key = it[Migrations.key],
                fromPublisher = it[Migrations.fromPublisher],
                toPublisher = it[Migrations.toPublisher],
                scheme = it[Migrations.scheme],
                identifier = it[Migrations.identifier],
            )
        }
    }

    fun delete(key: String) = transaction(database) {
        Migrations.deleteWhere {
            Migrations.key eq key
        }
    }
}

// Simple Participant identifier model
data class ParticipantIdentifier(
    val publisherId: String,
    val scheme: String,
    val identifier: String,
)

class ParticipantService(
    private val database: Database,
) {
    private val logger = LoggerFactory.getLogger(ParticipantService::class.java)

    object Participants : Table() {
        val id = integer("id").autoIncrement()
        val publisherId = varchar("publisher_id", 200).index()
        val scheme = varchar("scheme", 200)
        val identifier = varchar("identifier", 200)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Participants)
        }
    }

    fun create(p: ParticipantIdentifier): Int =
        transaction(database) {
            logger.debug("Creating participant: {}", p)
            Participants.insert {
                it[publisherId] = p.publisherId
                it[scheme] = p.scheme
                it[identifier] = p.identifier
            }[Participants.id]
        }

    fun delete(
        publisherIdStr: String,
        schemeStr: String,
        identifierStr: String,
    ): Boolean =
        transaction(database) {
            logger.debug("Deleting participant: {}", publisherIdStr)
            val deleted =
                Participants.deleteWhere {
                    (Participants.publisherId eq publisherIdStr) and (Participants.scheme eq schemeStr) and
                        (Participants.identifier eq identifierStr)
                }
            deleted > 0
        }

    fun listByPublisher(
        publisherIdStr: String,
        pageSize: Int = 100,
    ): List<ParticipantIdentifier> =
        transaction(database) {
            logger.debug("Listing participants for publisher: {}", publisherIdStr)
            Participants
                .selectAll()
                .where { Participants.publisherId eq publisherIdStr }
                .limit(pageSize)
                .map {
                    ParticipantIdentifier(
                        publisherId = it[Participants.publisherId],
                        scheme = it[Participants.scheme],
                        identifier = it[Participants.identifier],
                    )
                }
        }
}