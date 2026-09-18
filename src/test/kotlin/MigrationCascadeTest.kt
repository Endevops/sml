package be.endevops

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MigrationCascadeTest {
    @Test
    fun testDeleteParticipantCascadesToMigrationRecord() =
        testApplication {
            configure()
            application { testModule() }

            suspend fun postSoap(path: String, xml: String): String {
                val response =
                    client.post(path) {
                        contentType(ContentType.Text.Xml)
                        setBody(xml)
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                return response.body<String>()
            }

            @Language("xml")
            val createPublisher =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://example.org/cascade-pub</ns:LogicalAddress>
                        <ns:PhysicalAddress>cascade-pub</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>cascade-pub</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val publisherBody = postSoap("/manage-service-metadata", createPublisher)
            assertTrue(
                publisherBody.contains("<Result>OK</Result>"),
                "Expected OK creating publisher; got: $publisherBody",
            )

            @Language("xml")
            val createParticipant =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:CreateParticipantIdentifier>
                      <ns:ServiceMetadataPublisherID>cascade-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC1</ids:ParticipantIdentifier>
                    </ns:CreateParticipantIdentifier>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val createBody = postSoap("/manage-business-identifier", createParticipant)
            assertTrue(
                createBody.contains("<Result>OK</Result>"),
                "Expected OK creating participant; got: $createBody",
            )

            @Language("xml")
            val prepareMigration =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>cascade-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC1</ids:ParticipantIdentifier>
                      <ns:MigrationKey>cascade-key</ns:MigrationKey>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val prepareBody = postSoap("/manage-business-identifier", prepareMigration)
            assertTrue(
                prepareBody.contains("<Result>OK</Result>"),
                "Expected OK preparing migration; got: $prepareBody",
            )

            val beforeBody = client.get("/").body<String>()
            assertTrue(
                beforeBody.contains("cascade-key"),
                "Expected cascade-key before delete; got: $beforeBody",
            )

            @Language("xml")
            val deleteParticipant =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:DeleteParticipantIdentifier>
                      <ns:ServiceMetadataPublisherID>cascade-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC1</ids:ParticipantIdentifier>
                    </ns:DeleteParticipantIdentifier>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val deleteBody = postSoap("/manage-business-identifier", deleteParticipant)
            assertTrue(
                deleteBody.contains("<Result>OK</Result>"),
                "Expected OK deleting participant; got: $deleteBody",
            )

            val afterBody = client.get("/").body<String>()
            assertFalse(
                afterBody.contains("cascade-key"),
                "Expected cascade-key removed after participant delete; got: $afterBody",
            )
        }

    @Test
    fun testDeleteListCascadesToMigrationRecords() =
        testApplication {
            configure()
            application { testModule() }

            suspend fun postSoap(path: String, xml: String): String {
                val response =
                    client.post(path) {
                        contentType(ContentType.Text.Xml)
                        setBody(xml)
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                return response.body<String>()
            }

            @Language("xml")
            val createPublisher =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://example.org/cascade-batch-pub</ns:LogicalAddress>
                        <ns:PhysicalAddress>cascade-batch-pub</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>cascade-batch-pub</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val publisherBody = postSoap("/manage-service-metadata", createPublisher)
            assertTrue(
                publisherBody.contains("<Result>OK</Result>"),
                "Expected OK creating publisher; got: $publisherBody",
            )

            @Language("xml")
            val createList =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:CreateList>
                      <ns:ServiceMetadataPublisherID>cascade-batch-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-A</ids:ParticipantIdentifier>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-B</ids:ParticipantIdentifier>
                    </ns:CreateList>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val createBody = postSoap("/manage-business-identifier", createList)
            assertTrue(
                createBody.contains("<Result>OK</Result>"),
                "Expected OK creating participant list; got: $createBody",
            )

            @Language("xml")
            val prepareA =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>cascade-batch-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-A</ids:ParticipantIdentifier>
                      <ns:MigrationKey>cascade-key-a</ns:MigrationKey>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            @Language("xml")
            val prepareB =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>cascade-batch-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-B</ids:ParticipantIdentifier>
                      <ns:MigrationKey>cascade-key-b</ns:MigrationKey>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            assertTrue(
                postSoap("/manage-business-identifier", prepareA).contains("<Result>OK</Result>"),
                "Expected OK preparing migration for CASC-A",
            )
            assertTrue(
                postSoap("/manage-business-identifier", prepareB).contains("<Result>OK</Result>"),
                "Expected OK preparing migration for CASC-B",
            )

            val beforeBody = client.get("/").body<String>()
            assertTrue(beforeBody.contains("cascade-key-a"), "Expected cascade-key-a before delete; got: $beforeBody")
            assertTrue(beforeBody.contains("cascade-key-b"), "Expected cascade-key-b before delete; got: $beforeBody")

            @Language("xml")
            val deleteList =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:DeleteList>
                      <ns:ServiceMetadataPublisherID>cascade-batch-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-A</ids:ParticipantIdentifier>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">CASC-B</ids:ParticipantIdentifier>
                    </ns:DeleteList>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val deleteBody = postSoap("/manage-business-identifier", deleteList)
            assertTrue(
                deleteBody.contains("<Result>OK</Result>"),
                "Expected OK deleting participant list; got: $deleteBody",
            )

            val afterBody = client.get("/").body<String>()
            assertFalse(afterBody.contains("cascade-key-a"), "Expected cascade-key-a removed; got: $afterBody")
            assertFalse(afterBody.contains("cascade-key-b"), "Expected cascade-key-b removed; got: $afterBody")
        }
}
