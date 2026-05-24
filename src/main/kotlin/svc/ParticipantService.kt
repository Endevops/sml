package be.endevops.svc

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

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

    suspend fun create(p: ParticipantIdentifier): Int =
        suspendTransaction(database) {
            logger.debug("Creating participant: {}", p)
            Participants.insert {
                it[publisherId] = p.publisherId
                it[scheme] = p.scheme
                it[identifier] = p.identifier
            }[Participants.id]
        }

    suspend fun delete(
        publisherIdStr: String,
        schemeStr: String,
        identifierStr: String,
    ): Boolean =
        suspendTransaction(database) {
            logger.debug("Deleting participant: {}", publisherIdStr)
            val deleted =
                Participants.deleteWhere {
                    (Participants.publisherId eq publisherIdStr) and (Participants.scheme eq schemeStr) and
                        (Participants.identifier eq identifierStr)
                }
            deleted > 0
        }

    suspend fun listByPublisher(
        publisherIdStr: String,
        pageSize: Int = 100,
    ): List<ParticipantIdentifier> =
        suspendTransaction(database) {
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

    suspend fun listAll(): List<ParticipantIdentifier> =
        suspendTransaction(database) {
            Participants.selectAll().map {
                ParticipantIdentifier(
                    publisherId = it[Participants.publisherId],
                    scheme = it[Participants.scheme],
                    identifier = it[Participants.identifier],
                )
            }
        }
}
