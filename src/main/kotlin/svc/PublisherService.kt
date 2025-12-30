package be.endevops.svc

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


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

    fun create(p: ServiceMetadataPublisher): Int {
        return transaction(database) {
            // return existing id if publisher_id already present
            val existing =
                Publishers
                    .selectAll()
                    .where { Publishers.publisherId eq p.publisherId }
                    .map { it[Publishers.id] }
                    .singleOrNull()
            if (existing != null) return@transaction existing

            Publishers.insert {
                it[publisherId] = p.publisherId
                it[logicalAddress] = p.logicalAddress
                it[physicalAddress] = p.physicalAddress
            }[Publishers.id]
        }
    }

    fun get(publisherIdStr: String): ServiceMetadataPublisher? =
        transaction(database) {
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
    fun exists(publisherId: String): Boolean =
        transaction(database) {
            Publishers
                .selectAll()
                .where { Publishers.publisherId eq publisherId }
                .count() > 0
        }

    fun update(p: ServiceMetadataPublisher): Boolean =
        transaction(database) {
            val updated =
                Publishers.update({ Publishers.publisherId eq p.publisherId }) {
                    it[logicalAddress] = p.logicalAddress
                    it[physicalAddress] = p.physicalAddress
                }
            updated > 0
        }

    fun deleteByPublisherId(publisherIdStr: String): Boolean =
        transaction(database) {
            val deleted = Publishers.deleteWhere { Publishers.publisherId eq publisherIdStr }
            deleted > 0
        }
}