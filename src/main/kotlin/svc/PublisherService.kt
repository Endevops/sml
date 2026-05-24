package be.endevops.svc

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class PublisherService(
    private val database: Database,
) {
    object Publishers : Table() {
        val id = integer("id").autoIncrement()
        val publisherId = varchar("publisher_id", 200).uniqueIndex()
        val logicalAddress = varchar("logical_address", 1000)
        val physicalAddress = varchar("physical_address", 1000)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Publishers)
        }
    }

    // Lightweight domain model for service metadata publisher
    data class ServiceMetadataPublisher(
        val publisherId: String,
        val logicalAddress: String,
        val physicalAddress: String,
    )

    suspend fun create(p: ServiceMetadataPublisher): Int = suspendTransaction(database) {
        // return existing id if publisher_id already present
        val existing =
            Publishers
                .selectAll()
                .where { Publishers.publisherId eq p.publisherId }
                .map { it[Publishers.id] }
                .singleOrNull()
        if (existing != null) return@suspendTransaction existing

        Publishers.insert {
            it[publisherId] = p.publisherId
            it[logicalAddress] = p.logicalAddress
            it[physicalAddress] = p.physicalAddress
        }[Publishers.id]
    }

    suspend fun get(publisherIdStr: String): ServiceMetadataPublisher? =
        suspendTransaction(database) {
            Publishers
                .selectAll()
                .where { Publishers.publisherId eq publisherIdStr }
                .map {
                    ServiceMetadataPublisher(
                        publisherId = it[Publishers.publisherId],
                        logicalAddress = it[Publishers.logicalAddress],
                        physicalAddress = it[Publishers.physicalAddress],
                    )
                }.singleOrNull()
        }

    /**
     * Check if a publisher with the given publisherId exists in the database.
     */
    suspend fun exists(publisherId: String): Boolean =
        suspendTransaction(database) {
            Publishers
                .selectAll()
                .where { Publishers.publisherId eq publisherId }
                .count() > 0
        }

    suspend fun update(p: ServiceMetadataPublisher): Boolean =
        suspendTransaction(database) {
            val updated =
                Publishers.update({ Publishers.publisherId eq p.publisherId }) {
                    it[logicalAddress] = p.logicalAddress
                    it[physicalAddress] = p.physicalAddress
                }
            updated > 0
        }

    suspend fun deleteByPublisherId(publisherIdStr: String): Boolean =
        suspendTransaction(database) {
            val deleted = Publishers.deleteWhere { Publishers.publisherId eq publisherIdStr }
            deleted > 0
        }

    suspend fun listAll(): List<ServiceMetadataPublisher> =
        suspendTransaction(database) {
            Publishers.selectAll().map {
                ServiceMetadataPublisher(
                    publisherId = it[Publishers.publisherId],
                    logicalAddress = it[Publishers.logicalAddress],
                    physicalAddress = it[Publishers.physicalAddress],
                )
            }
        }
}
