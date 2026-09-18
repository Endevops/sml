package be.endevops.svc

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationServiceTest {
    private fun newMigrationService(): MigrationService {
        val temp: Path = createTempFile("migtest", ".db")
        temp.deleteExisting()
        val db = Database.connect("jdbc:sqlite:$temp", "org.sqlite.JDBC")
        return MigrationService(db)
    }

    @Test
    fun deleteByParticipantRemovesOnlyExactMatches() =
        runBlocking {
            val service = newMigrationService()
            val sharedPublisher = "shared-pub"
            val sharedScheme = "iso6523-actorid-upis"
            val sharedIdentifier = "0010:shared"

            service.create(
                MigrationService.MigrationRecord("k1", sharedPublisher, sharedScheme, sharedIdentifier),
            )
            service.create(
                MigrationService.MigrationRecord("k2", sharedPublisher, sharedScheme, sharedIdentifier),
            )
            service.create(
                MigrationService.MigrationRecord("k3", "other-pub", sharedScheme, sharedIdentifier),
            )
            service.create(
                MigrationService.MigrationRecord("k4", sharedPublisher, "other-scheme", sharedIdentifier),
            )
            service.create(
                MigrationService.MigrationRecord("k5", sharedPublisher, sharedScheme, "0010:other"),
            )

            val deleted =
                service.deleteByParticipant(sharedPublisher, sharedScheme, sharedIdentifier)
            assertEquals(2, deleted, "Expected exactly the two full matches to be deleted")

            val remaining = service.listAll().toSet()
            assertEquals(
                setOf(
                    MigrationService.MigrationRecord("k3", "other-pub", sharedScheme, sharedIdentifier),
                    MigrationService.MigrationRecord("k4", sharedPublisher, "other-scheme", sharedIdentifier),
                    MigrationService.MigrationRecord("k5", sharedPublisher, sharedScheme, "0010:other"),
                ),
                remaining,
                "Records with a different publisher, scheme or identifier must survive",
            )
        }

    @Test
    fun deleteByKeyAndPublisherRemovesOnlyOneCompositeRow() =
        runBlocking {
            val service = newMigrationService()
            val key = "dup-key"
            val scheme = "iso6523-actorid-upis"
            val identifier = "0010:shared"

            service.create(MigrationService.MigrationRecord(key, "pub-a", scheme, identifier))
            service.create(MigrationService.MigrationRecord(key, "pub-b", scheme, identifier))

            val deleted = service.deleteByKeyAndPublisher(key, "pub-a")
            assertEquals(1, deleted, "Only the (key, pub-a) composite row should be deleted")

            assertEquals(
                listOf(MigrationService.MigrationRecord(key, "pub-b", scheme, identifier)),
                service.listAll(),
                "The same key under another publisher must survive",
            )
        }
}
