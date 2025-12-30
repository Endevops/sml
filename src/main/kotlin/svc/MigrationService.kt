package be.endevops.svc

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

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

    fun create(r: MigrationRecord) =
        transaction(database) {
            Migrations.insert {
                it[key] = r.key
                it[fromPublisher] = r.fromPublisher
                it[scheme] = r.scheme
                it[identifier] = r.identifier
            }[Migrations.key]
        }

    fun get(key: String) =
        transaction(database) {
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

    fun delete(key: String) =
        transaction(database) {
            Migrations.deleteWhere {
                Migrations.key eq key
            }
        }

    fun listAll(): List<MigrationRecord> =
        transaction(database) {
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
