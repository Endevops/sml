package be.endevops

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.testApplication
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugUiMigrationDeleteTest {
    @Test
    fun testDebugUiDeletesMigrationByKeyAndPublisher() =
        testApplication {
            configure()
            application { testModule() }

            @Language("xml")
            val prepareReq =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>ui-test-pub</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">UI1</ids:ParticipantIdentifier>
                      <ns:MigrationKey>ui-migrate-key</ns:MigrationKey>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            val prepareResp =
                client.post("/manage-business-identifier") {
                    contentType(ContentType.Text.Xml)
                    setBody(prepareReq)
                }
            assertEquals(HttpStatusCode.OK, prepareResp.status)
            val prepareBody = prepareResp.body<String>()
            assertTrue(
                prepareBody.contains("<Result>OK</Result>"),
                "Expected OK for prepare migration; got: $prepareBody",
            )

            val beforeBody = client.get("/").body<String>()
            assertTrue(
                beforeBody.contains("ui-migrate-key"),
                "Expected migration key in debug UI before delete; got: $beforeBody",
            )

            val deleteResp =
                client.post("/debug/migrations/delete") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf("key" to "ui-migrate-key", "fromPublisher" to "ui-test-pub").formUrlEncode(),
                    )
                }
            assertEquals(HttpStatusCode.Found, deleteResp.status)

            val afterBody = client.get("/").body<String>()
            assertFalse(
                afterBody.contains("ui-migrate-key"),
                "Expected migration key removed from debug UI after delete; got: $afterBody",
            )
        }

    @Test
    fun testDebugUiDeleteDoesNotDeleteByKeyAlone() =
        testApplication {
            configure()
            application { testModule() }

            @Language("xml")
            val prepareTemplate =
                """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns="http://busdox.org/serviceMetadata/locator/1.0/"
                  xmlns:ids="http://busdox.org/transport/identifiers/1.0/">
                  <soap:Body>
                    <ns:PrepareMigrationRecord>
                      <ns:ServiceMetadataPublisherID>PUBLISHER</ns:ServiceMetadataPublisherID>
                      <ids:ParticipantIdentifier scheme="iso6523-actorid-upis">IDENTIFIER</ids:ParticipantIdentifier>
                      <ns:MigrationKey>shared-neg-key</ns:MigrationKey>
                    </ns:PrepareMigrationRecord>
                  </soap:Body>
                </soap:Envelope>
                """.trimIndent()

            fun prepare(publisher: String, identifier: String): String =
                prepareTemplate
                    .replace("PUBLISHER", publisher)
                    .replace("IDENTIFIER", identifier)

            for ((publisher, identifier) in listOf("ui-neg-a" to "NEG-A", "ui-neg-b" to "NEG-B")) {
                val resp =
                    client.post("/manage-business-identifier") {
                        contentType(ContentType.Text.Xml)
                        setBody(prepare(publisher, identifier))
                    }
                assertEquals(HttpStatusCode.OK, resp.status)
                val body = resp.body<String>()
                assertTrue(
                    body.contains("<Result>OK</Result>"),
                    "Expected OK preparing $publisher; got: $body",
                )
            }

            val beforeBody = client.get("/").body<String>()
            assertTrue(beforeBody.contains("NEG-A"), "Expected NEG-A before delete; got: $beforeBody")
            assertTrue(beforeBody.contains("NEG-B"), "Expected NEG-B before delete; got: $beforeBody")

            val deleteResp =
                client.post("/debug/migrations/delete") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf("key" to "shared-neg-key", "fromPublisher" to "ui-neg-a").formUrlEncode(),
                    )
                }
            assertEquals(HttpStatusCode.Found, deleteResp.status)

            val afterBody = client.get("/").body<String>()
            assertFalse(afterBody.contains("NEG-A"), "Expected NEG-A removed after delete; got: $afterBody")
            assertTrue(afterBody.contains("NEG-B"), "Expected NEG-B kept after delete; got: $afterBody")
        }

    @Test
    fun testDebugUiDeleteWithoutFieldsReturnsBadRequest() =
        testApplication {
            configure()
            application { testModule() }

            val response = client.post("/debug/migrations/delete")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
