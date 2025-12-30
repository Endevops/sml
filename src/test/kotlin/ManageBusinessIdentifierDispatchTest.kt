package be.endevops

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManageBusinessIdentifierDispatchTest {
    @Test
    fun testCreateListDeleteParticipant() =
        testApplication {
            application { testModule() }

            val createReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/" xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:CreateParticipantIdentifier>
                      <ns:ServiceMetadataPublisherID>test-pub-1</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier>
                        <ids:scheme>iso6523-actorid-upis</ids:scheme>
                        <ids:identifier>12345</ids:identifier>
                      </ids:ParticipantIdentifier>
                    </ns:CreateParticipantIdentifier>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val createResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(createReq)
                }
            assertEquals(HttpStatusCode.OK, createResp.status)
            val createBody = createResp.body<String>()
            assertTrue(createBody.contains("<Result>OK</Result>"), "Expected Result OK in create response, got: $createBody")
            assertTrue(
                Regex("<DatabaseId>\\d+</DatabaseId>").containsMatchIn(createBody),
                "Expected numeric DatabaseId in response, got: $createBody",
            )

            val listReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:PageRequest>
                      <ns:ServiceMetadataPublisherID>test-pub-1</ns:ServiceMetadataPublisherID>
                    </ns:PageRequest>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val listResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(listReq)
                }
            assertEquals(HttpStatusCode.OK, listResp.status)
            val listBody = listResp.body<String>()
            assertTrue(listBody.contains("<ParticipantIdentifier>"), "Expected participant in list response, got: $listBody")
            assertTrue(listBody.contains("<identifier>12345</identifier>"), "Expected identifier 12345 in list response, got: $listBody")

            val deleteReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/" xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:DeleteParticipantIdentifier>
                      <ns:ServiceMetadataPublisherID>test-pub-1</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier>
                        <ids:scheme>iso6523-actorid-upis</ids:scheme>
                        <ids:identifier>12345</ids:identifier>
                      </ids:ParticipantIdentifier>
                    </ns:DeleteParticipantIdentifier>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val deleteResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(deleteReq)
                }
            assertEquals(HttpStatusCode.OK, deleteResp.status)
            val deleteBody = deleteResp.body<String>()
            assertTrue(deleteBody.contains("<Result>OK</Result>"), "Expected Result OK in delete response, got: $deleteBody")

            val listResp2 =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(listReq)
                }
            assertEquals(HttpStatusCode.OK, listResp2.status)
            val listBody2 = listResp2.body<String>()
            assertTrue(!listBody2.contains("<identifier>12345</identifier>"), "Expected identifier removed after delete, got: $listBody2")
        }

    @Test
    fun testCreateListAndMigrate() =
        testApplication {
            application { testModule() }

            val createListReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/" xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:CreateList>
                      <ns:ServiceMetadataPublisherID>bulk-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier>
                        <ids:scheme>iso6523-actorid-upis</ids:scheme>
                        <ids:identifier>A1</ids:identifier>
                      </ids:ParticipantIdentifier>
                      <ids:ParticipantIdentifier>
                        <ids:scheme>iso6523-actorid-upis</ids:scheme>
                        <ids:identifier>A2</ids:identifier>
                      </ids:ParticipantIdentifier>
                    </ns:CreateList>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val resp1 =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(createListReq)
                }
            assertEquals(HttpStatusCode.OK, resp1.status)
            val body1 = resp1.body<String>()
            assertTrue(body1.contains("<Result>OK</Result>"), "Expected OK in create list response; got: $body1")

            val listReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:PageRequest>
                      <ns:ServiceMetadataPublisherID>bulk-pub</ns:ServiceMetadataPublisherID>
                    </ns:PageRequest>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val listResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(listReq)
                }
            val listBody = listResp.body<String>()
            assertTrue(listBody.contains("<identifier>A1</identifier>"), "Expected A1 in list; got: $listBody")
            assertTrue(listBody.contains("<identifier>A2</identifier>"), "Expected A2 in list; got: $listBody")

            val prepareReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/" xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>bulk-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier>
                        <ids:scheme>iso6523-actorid-upis</ids:scheme>
                        <ids:identifier>A1</ids:identifier>
                      </ids:ParticipantIdentifier>
                      <ns:MigrationKey>migrate-A1</ns:MigrationKey>
                      <ns:ToServiceMetadataPublisherID>target-pub</ns:ToServiceMetadataPublisherID>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val prepResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(prepareReq)
                }
            val prepBody = prepResp.body<String>()
            assertTrue(prepBody.contains("<Result>OK</Result>"), "Expected OK for prepare migrate; got: $prepBody")

            val migrateReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CompleteMigrationRecord>
                      <ns:MigrationKey>migrate-A1</ns:MigrationKey>
                    </ns:CompleteMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val migResp =
                client.post("/sml/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(migrateReq)
                }
            val migBody = migResp.body<String>()
            assertTrue(migBody.contains("<Result>OK</Result>"), "Expected OK from migrate; got: $migBody")

            val listBulk =
                client
                    .post("/sml/manage-business-identifier") {
                        contentType(ContentType.Text.Xml)
                        setBody(listReq)
                    }.body<String>()
            assertTrue(!listBulk.contains("<identifier>A1</identifier>"), "Expected A1 removed from source; got: $listBulk")

            val listReqTarget =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:PageRequest>
                      <ns:ServiceMetadataPublisherID>target-pub</ns:ServiceMetadataPublisherID>
                    </ns:PageRequest>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val listTarget =
                client
                    .post("/sml/manage-business-identifier") {
                        contentType(ContentType.Text.Xml)
                        setBody(listReqTarget)
                    }.body<String>()
            assertTrue(listTarget.contains("<identifier>A1</identifier>"), "Expected A1 present under target; got: $listTarget")
        }
}
