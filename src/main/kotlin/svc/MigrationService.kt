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

class MigrationService(
    private val database: Database,
) {
    object Migrations : Table() {
        val key = varchar("key", 200)
        val fromPublisher = varchar("from_publisher", 200)
        val scheme = varchar("scheme", 100)
        val identifier = varchar("identifier", 200)

        override val primaryKey = PrimaryKey(key, fromPublisher)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Migrations)
        }
    }

    data class MigrationRecord(
        val key: String,
        val fromPublisher: String,
        val scheme: String,
        val identifier: String,
    )

    suspend fun create(r: MigrationRecord) =
        suspendTransaction(database) {
            Migrations.insert {
                it[key] = r.key
                it[fromPublisher] = r.fromPublisher
                it[scheme] = r.scheme
                it[identifier] = r.identifier
            }[Migrations.key]
        }

    suspend fun get(key: String) =
        suspendTransaction(database) {
            Migrations
                .selectAll()
                .where {
                    Migrations.key eq key
                }.singleOrNull()
                ?.let {
                    MigrationRecord(
                        key = it[Migrations.key],
                        fromPublisher = it[Migrations.fromPublisher],
                        scheme = it[Migrations.scheme],
                        identifier = it[Migrations.identifier],
                    )
                }
        }

    suspend fun delete(key: String) =
        suspendTransaction(database) {
            Migrations.deleteWhere {
                Migrations.key eq key
            }
        }

    suspend fun listAll(): List<MigrationRecord> =
        suspendTransaction(database) {
            Migrations.selectAll().map {
                MigrationRecord(
                    key = it[Migrations.key],
                    fromPublisher = it[Migrations.fromPublisher],
                    scheme = it[Migrations.scheme],
                    identifier = it[Migrations.identifier],
                )
            }
        }
}
