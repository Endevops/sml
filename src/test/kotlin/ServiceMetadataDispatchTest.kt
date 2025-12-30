package be.endevops

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceMetadataDispatchTest {
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
                client.post("/sml/manage-service-metadata") {
                    contentType(ContentType.Text.Xml)
                    setBody(soapRequest)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<String>()
            assertTrue(body.contains("<Result>OK</Result>"), "Expected Result OK in response, got: $body")
            assertTrue(Regex("<DatabaseId>\\d+</DatabaseId>").containsMatchIn(body), "Expected numeric DatabaseId in response, got: $body")
        }
}
