package be.endevops

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceMetadataEndpointsTest {
    @Test
    fun testCreateServiceMetadataPublisher() =
        testApplication {
            application { testModule() }

            val soapRequest =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://example.org/logical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Example Publisher</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>test-pub-1</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val response =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected Result OK in response, got: $body")
            assertTrue(
                Regex("<DatabaseId>\\d+</DatabaseId>").containsMatchIn(body),
                "Expected numeric DatabaseId in response, got: $body",
            )
        }

    @Test
    fun testCreateIdempotent() =
        testApplication {
            application { testModule() }

            val soapRequest =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://example.org/logical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Example Publisher</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>test-pub-2</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val r1 =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }
            val b1 = r1.bodyAsText()
            assertTrue(Regex("<DatabaseId>(\\d+)</DatabaseId>").find(b1) != null)
            val id1 = Regex("<DatabaseId>(\\d+)</DatabaseId>").find(b1)!!.groupValues[1]

            val r2 =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }
            val b2 = r2.bodyAsText()
            val id2 = Regex("<DatabaseId>(\\d+)</DatabaseId>").find(b2)!!.groupValues[1]

            assertEquals(id1, id2, "Expected idempotent create to return same DatabaseId for same publisher id")
        }

    @Test
    fun testReadEndpointReturnsOk() =
        testApplication {
            application { testModule() }

            val soapRequest =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:ReadServiceMetadataPublisherService>
                      <ns:ServiceMetadataPublisherID>does-not-matter</ns:ServiceMetadataPublisherID>
                    </ns:ReadServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val response =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected Result OK in response, got: $body")
        }

    @Test
    fun testUpdateEndpointReturnsOk() =
        testApplication {
            application { testModule() }

            val soapRequest =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:UpdateServiceMetadataPublisherService>
                      <ns:ServiceMetadataPublisherID>test-pub-update</ns:ServiceMetadataPublisherID>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://persistence.test/logical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Persistence Publisher</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                    </ns:UpdateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val response =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected Result OK in response, got: $body")
        }

    @Test
    fun testDeleteEndpointReturnsOk() =
        testApplication {
            application { testModule() }

            val soapRequest =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:DeleteServiceMetadataPublisherService>
                      <ns:ServiceMetadataPublisherID>test-pub-delete</ns:ServiceMetadataPublisherID>
                    </ns:DeleteServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val response =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected Result OK in response, got: $body")
        }

    @Test
    fun testCreateThenReadPersistence() =
        testApplication {
            application { testModule() }

            val create =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://persistence.test/logical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Persistence Publisher</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>persist-1</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val r1 =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(create)
                }
            assertEquals(HttpStatusCode.OK, r1.status)

            val read =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:ReadServiceMetadataPublisherService>
                      <ns:ServiceMetadataPublisherID>persist-1</ns:ServiceMetadataPublisherID>
                    </ns:ReadServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val rr =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(read)
                }
            val body = rr.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected OK in read response, got: $body")
            assertTrue(
                body.contains("<ServiceMetadataPublisherID>persist-1</ServiceMetadataPublisherID>"),
                "Expected id in read response, got: $body",
            )
            assertTrue(
                body.contains("<LogicalAddress>http://persistence.test/logical</LogicalAddress>"),
                "Expected logical in response, got: $body",
            )
            assertTrue(
                body.contains("<PhysicalAddress>Persistence Publisher</PhysicalAddress>"),
                "Expected physical in response, got: $body",
            )
        }

    @Test
    fun testCreateUpdateReadPersistence() =
        testApplication {
            application { testModule() }

            val create =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:CreateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://update.test/logical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Update Publisher</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>upd-1</ns:ServiceMetadataPublisherID>
                    </ns:CreateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            client.post("/manage-service-metadata") {
                contentType(ContentType.Text.Xml)
                setBody(create)
            }

            val update =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:UpdateServiceMetadataPublisherService>
                      <ns:PublisherEndpoint>
                        <ns:LogicalAddress>http://update.test/newlogical</ns:LogicalAddress>
                        <ns:PhysicalAddress>Update Publisher v2</ns:PhysicalAddress>
                      </ns:PublisherEndpoint>
                      <ns:ServiceMetadataPublisherID>upd-1</ns:ServiceMetadataPublisherID>
                    </ns:UpdateServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val up =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(update)
                }
            assertEquals(HttpStatusCode.OK, up.status)

            val read =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/">
                  <soap:Body>
                    <ns:ReadServiceMetadataPublisherService>
                      <ns:ServiceMetadataPublisherID>upd-1</ns:ServiceMetadataPublisherID>
                    </ns:ReadServiceMetadataPublisherService>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val rr =
                client.post("/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(read)
                }
            val body = rr.bodyAsText()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected OK in read response, got: $body")
            assertTrue(
                body.contains("<LogicalAddress>http://update.test/newlogical</LogicalAddress>"),
                "Expected updated logical, got: $body",
            )
            assertTrue(
                body.contains("<PhysicalAddress>Update Publisher v2</PhysicalAddress>"),
                "Expected updated physical, got: $body",
            )
        }
}
