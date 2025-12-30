package be.endevops

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

private const val BUSINESS_IDENTIFIER_NS = "http://busdox.org/serviceMetadata/locator/1.0/"

fun Application.configureBusinessIdentifier() {
    routing {
        get("/sml/manage-business-identifier") {
            val wsdlRequested = call.request.queryParameters.contains("wsdl")
            if (wsdlRequested) {
                val resource =
                    this::class.java.classLoader.getResourceAsStream("wsdl/peppol-sml-manage-business-identifier-service-v1.wsdl")
                if (resource != null) {
                    val content = resource.readBytes()
                    call.respondBytes(content, ContentType.Text.Xml.withCharset(Charsets.UTF_8), HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } else {
                call.respond(HttpStatusCode.OK)
            }
        }
        post("/sml/manage-business-identifier") {
            val raw = call.receiveText()
            val soapAction = call.request.header("SOAPAction")
            val responseXml = dispatchManageBusinessIdentifier(raw, soapAction)
            call.respondText(responseXml, ContentType.Text.Xml.withCharset(Charsets.UTF_8), HttpStatusCode.OK)
        }
    }
}

suspend fun Application.dispatchManageBusinessIdentifier(
    requestXml: String,
    soapActionHeader: String?,
): String {
    log.trace(
        "dispatchManageBusinessIdentifier called: soapAction='{}' requestLength={}",
        soapActionHeader,
        requestXml.length,
    )
    val operation = extractOperationNameFromSoap(requestXml) ?: extractOperationFromSoapAction(soapActionHeader)
    log.info("Determined operation='{}'", operation)

    val inner =
        when (operation) {
            "CreateParticipantIdentifier", "Create" -> {
                createParticipant(requestXml)
            }

            "DeleteParticipantIdentifier", "Delete" -> {
                deleteParticipant(requestXml)
            }

            "List", "ParticipantIdentifierPage", "PageRequest", "PageRequestType" -> {
                listParticipants(requestXml)
            }

            "CreateList", "CreateListIn", "CreateListType" -> {
                createListParticipants(requestXml)
            }

            "DeleteList", "DeleteListIn", "DeleteListType" -> {
                deleteParticipantList(requestXml)
            }

            "PrepareToMigrate", "PrepareMigrationRecord", "PrepareMigrationRecordType" -> {
                prepareToMigrate(requestXml)
            }

            "Migrate", "CompleteMigrationRecord", "CompleteMigrationRecordType" -> {
                migrate(requestXml)
            }

            else -> {
                log.warn("Unknown operation='{}'", operation)
                "<Fault><FaultMessage>Unsupported operation: ${operation ?: "Unknown"}</FaultMessage></Fault>"
            }
        }

    return wrapInSoapEnvelope(inner)
}

private fun extractOperationFromSoapAction(soapAction: String?): String? {
    if (soapAction == null) return null
    val idx = soapAction.lastIndexOf(':')
    if (idx >= 0 && idx < soapAction.length - 1) {
        return soapAction
            .substring(idx + 1)
            .replace("In", "")
            .replace("In\"", "")
            .trim()
    }
    return soapAction
}

private fun Application.extractOperationNameFromSoap(xml: String): String? {
    return try {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        log.trace("extractOperationNameFromSoap: found Body nodeList length={}", nodeList?.length ?: 0)
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) {
                    val local = n.localName ?: n.nodeName
                    log.trace(
                        "extractOperationNameFromSoap: found body child element localName='{}' namespace='{}'",
                        local,
                        n.namespaceURI,
                    )
                    try {
                        log.trace("extractOperationNameFromSoap: element xml={}", nodeToString(n))
                    } catch (t: Throwable) {
                        log.trace("extractOperationNameFromSoap: nodeToString failed: {}", t.message)
                    }
                    return n.localName ?: n.nodeName
                }
            }
        }
        null
    } catch (e: Exception) {
        log.trace("Failed to extract operation name: {}", e.message)
        null
    }
}

private fun Application.firstElementInSoapBody(xml: String): Element? {
    return try {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        log.trace("firstElementInSoapBody: found Body nodeList length={}", nodeList?.length ?: 0)
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) {
                    try {
                        log.trace(
                            "firstElementInSoapBody: returning element local='{}' ns='{}' xml={}",
                            n.localName ?: n.nodeName,
                            n.namespaceURI,
                            nodeToString(n),
                        )
                    } catch (t: Throwable) {
                        log.trace("firstElementInSoapBody: nodeToString failed: {}", t.message)
                    }
                    return n
                }
            }
        }
        null
    } catch (e: Exception) {
        log.trace("Failed to parse SOAP body first element: {}", e.message)
        null
    }
}

private fun nodeToString(node: Element): String {
    val sw = java.io.StringWriter()
    val transformer =
        javax.xml.transform.TransformerFactory
            .newInstance()
            .newTransformer()
    transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes")
    val src =
        javax.xml.transform.dom
            .DOMSource(node)
    val out =
        javax.xml.transform.stream
            .StreamResult(sw)
    transformer.transform(src, out)
    return sw.toString()
}

private fun Application.createParticipant(requestXml: String): String {
    log.trace("handleCreate entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, CreateParticipantIdentifierRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    var publisherId = ""
    var scheme = ""
    var identifier = ""
    if (req != null) {
        publisherId = req.serviceMetadataPublisherID ?: ""
        val pid = req.participantIdentifier
        if (pid != null) {
            scheme = pid.scheme ?: ""
            identifier = pid.identifier ?: ""
        }
    }

    if (publisherId.isBlank() || identifier.isBlank()) {
        return "<BadRequestFault><FaultMessage>Missing required fields</FaultMessage></BadRequestFault>"
    }

    val participantService: ParticipantService by dependencies
    val dbId = participantService.create(ParticipantIdentifier(publisherId, scheme, identifier))
    log.trace("handleCreate createdId={}", dbId)
    return "<Result>OK</Result><DatabaseId>$dbId</DatabaseId>"
}

private fun Application.deleteParticipant(requestXml: String): String {
    log.trace("handleDelete entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val decoded = JsonXmlConverter.fromXmlString(opXml)

    var publisherId = ""
    var scheme = ""
    var identifier = ""
    if (decoded is JsonObject) {
        publisherId = decoded["ServiceMetadataPublisherID"]?.jsonPrimitive?.content ?: ""
        val pid = decoded["ParticipantIdentifier"]
        if (pid is JsonObject) {
            scheme = pid["scheme"]?.jsonPrimitive?.content ?: pid["Scheme"]?.jsonPrimitive?.content ?: ""
            identifier = pid["identifier"]?.jsonPrimitive?.content ?: pid["Identifier"]?.jsonPrimitive?.content ?: ""
        }
    }

    if (publisherId.isBlank() ||
        identifier.isBlank()
    ) {
        return "<BadRequestFault><FaultMessage>Missing required fields</FaultMessage></BadRequestFault>"
    }
    val participantService: ParticipantService by dependencies
    val deleted = participantService.delete(publisherId, scheme, identifier)
    log.trace("handleDelete deleted={}", deleted)
    return "<Result>OK</Result>"
}

private fun Application.listParticipants(requestXml: String): String {
    log.trace("listParticipants entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, PageRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    var publisherId = ""
    if (req != null) publisherId = req.serviceMetadataPublisherID ?: ""
    if (publisherId.isBlank()) return "<BadRequestFault><FaultMessage>Missing ServiceMetadataPublisherID</FaultMessage></BadRequestFault>"

    val participantService: ParticipantService by dependencies
    val results = participantService.listByPublisher(publisherId)
    val sb = StringBuilder()
    sb.append("<ParticipantIdentifierPage>")
    for (r in results) {
        sb.append(
            "<ParticipantIdentifier><scheme>${
                escapeXml(
                    r.scheme,
                )
            }</scheme><identifier>${escapeXml(r.identifier)}</identifier></ParticipantIdentifier>",
        )
    }
    sb.append("</ParticipantIdentifierPage>")
    return sb.toString()
}

private fun escapeXml(s: String): String =
    s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

// Simple in-memory migration store (keeps pending migrations keyed by migration key)
private object MigrationStore {
    private val store = java.util.concurrent.ConcurrentHashMap<String, MigrationRecord>()

    data class MigrationRecord(
        val fromPublisher: String,
        val toPublisher: String,
        val scheme: String,
        val identifier: String,
    )

    fun prepare(
        key: String,
        r: MigrationRecord,
    ) {
        store[key] = r
    }

    fun consume(key: String): MigrationRecord? = store.remove(key)
}

private fun Application.createListParticipants(requestXml: String): String {
    log.trace("createListParticipants entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, CreateListRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    var publisherId = ""
    val items = mutableListOf<ParticipantIdentifier>()
    if (req != null) {
        publisherId = req.serviceMetadataPublisherID ?: ""
        val pidArray = req.participantIdentifier
        if (!pidArray.isNullOrEmpty()) {
            for (elem in pidArray) {
                val scheme = elem.scheme ?: ""
                val identifier = elem.identifier ?: ""
                if (publisherId.isNotBlank() && identifier.isNotBlank()) {
                    items.add(ParticipantIdentifier(publisherId, scheme, identifier))
                }
            }
        }
    }

    if (items.isEmpty()) return "<BadRequestFault><FaultMessage>No items to create</FaultMessage></BadRequestFault>"
    val participantService: ParticipantService by dependencies
    for (it in items) participantService.create(it)
    return "<Result>OK</Result><Created>${items.size}</Created>"
}

private fun Application.deleteParticipantList(requestXml: String): String {
    log.trace("deleteParticipantList entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, DeleteListRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    var publisherId = ""
    var deletedCount = 0
    if (req != null) {
        publisherId = req.serviceMetadataPublisherID ?: ""
        val pidArray = req.participantIdentifier
        val participantService: ParticipantService by dependencies
        if (!pidArray.isNullOrEmpty()) {
            for (elem in pidArray) {
                val scheme = elem.scheme ?: ""
                val identifier = elem.identifier ?: ""
                if (publisherId.isNotBlank() && identifier.isNotBlank()) {
                    if (participantService.delete(publisherId, scheme, identifier)) {
                        deletedCount++
                    }
                }
            }
        }
    }

    return "<Result>OK</Result><Deleted>$deletedCount</Deleted>"
}

private fun Application.prepareToMigrate(requestXml: String): String {
    log.trace("prepareToMigrate entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, PrepareMigrationRecordRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    if (req != null) {
        val publisherId = req.serviceMetadataPublisherID ?: ""
        val pid = req.participantIdentifier
        val migrationKey =
            req.migrationKey ?: java.util.UUID
                .randomUUID()
                .toString()
        if (pid != null) {
            val scheme = pid.scheme ?: ""
            val identifier = pid.identifier ?: ""
            val toPublisher = req.toServiceMetadataPublisherID ?: ""
            if (publisherId.isNotBlank() && toPublisher.isNotBlank() && identifier.isNotBlank()) {
                MigrationStore.prepare(
                    migrationKey,
                    MigrationStore.MigrationRecord(publisherId, toPublisher, scheme, identifier),
                )
                return "<Result>OK</Result><MigrationKey>$migrationKey</MigrationKey>"
            }
        }
    }

    return "<BadRequestFault><FaultMessage>Invalid prepare migration</FaultMessage></BadRequestFault>"
}

private fun Application.migrate(requestXml: String): String {
    log.trace("migrate entry, preview={}", preview(requestXml))
    val opElement =
        firstElementInSoapBody(requestXml)
            ?: return "<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>"
    val opXml = nodeToString(opElement)
    val xmlMapper = XmlMapper.builder().findAndAddModules().build()
    val req =
        try {
            xmlMapper.readValue(opXml, CompleteMigrationRecordRequestPojo::class.java)
        } catch (e: Exception) {
            null
        }

    if (req != null) {
        val migrationKey = req.migrationKey ?: ""
        if (migrationKey.isNotBlank()) {
            val rec = MigrationStore.consume(migrationKey)
            if (rec != null) {
                val participantService: ParticipantService by dependencies
                participantService.delete(rec.fromPublisher, rec.scheme, rec.identifier)
                participantService.create(ParticipantIdentifier(rec.toPublisher, rec.scheme, rec.identifier))
                return "<Result>OK</Result>"
            }
        }
    }
    return "<BadRequestFault><FaultMessage>Invalid migrate request</FaultMessage></BadRequestFault>"
}

private fun preview(
    s: String?,
    max: Int = 512,
): String {
    if (s == null) return "null"
    return if (s.length <= max) s else s.substring(0, max) + "..."
}

private fun wrapInSoapEnvelope(innerXml: String): String =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
        "  <soap:Body>\n" +
        indentXml(innerXml) + "\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>"

private fun indentXml(
    xml: String,
    spaces: Int = 4,
): String {
    val pad = " ".repeat(spaces)
    return xml.lines().joinToString("\n") { if (it.isBlank()) it else pad + it }
}
