package be.endevops

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

// Lightweight dispatcher for the Manage Service Metadata SOAP interface.
// This file provides minimal handlers for Create/Read/Update/Delete and wraps results in a SOAP envelope.
const val SERVICE_METADATA_LOCATOR_URI = "http://busdox.org/serviceMetadata/locator/1.0/"

private val logger = LoggerFactory.getLogger("be.endevops.SoapDispatch")

private val xmlMapper: XmlMapper = XmlMapper.builder().findAndAddModules().build()

// Basic Kotlin representations used for dispatching (kept minimal)
data class PublisherEndpointType(
    val logicalAddress: String,
    val physicalAddress: String,
)

data class ServiceMetadataPublisherServiceType(
    val publisherEndpoint: PublisherEndpointType,
    val serviceMetadataPublisherID: String,
)

fun Application.configureManageServiceMetadata() {
    routing {
        get("/sml/manage-service-metadata") {
            // Serve WSDL when '?wsdl' is present; otherwise return OK
            val wsdlRequested = call.request.queryParameters.contains("wsdl")
            if (!wsdlRequested) {
                call.respond(HttpStatusCode.OK)
            } else {
                val resource =
                    this::class.java.classLoader.getResourceAsStream(
                        "wsdl/peppol-sml-manage-service-metadata-service-v1.wsdl",
                    )
                if (resource != null) {
                    val content = resource.readBytes()
                    call.respondBytes(content, ContentType.Text.Xml.withCharset(Charsets.UTF_8), HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        post("/sml/manage-service-metadata") {
            val raw = call.receiveText()
            val soapAction = call.request.header("SOAPAction")
            val responseXml = dispatchManageServiceMetadata(raw, soapAction)
            call.respondText(responseXml, ContentType.Text.Xml.withCharset(Charsets.UTF_8), HttpStatusCode.OK)
        }
    }
}

// Dispatcher entry point
suspend fun Application.dispatchManageServiceMetadata(
    requestXml: String,
    soapActionHeader: String?,
): String {
    logger.debug(
        "dispatchManageServiceMetadata called: soapActionHeader='{}', requestLength={}",
        soapActionHeader,
        requestXml.length,
    )
    val operation = extractOperationNameFromSoap(requestXml) ?: extractOperationFromSoapAction(soapActionHeader)
    logger.debug("Determined operation='{}'", operation)

    val innerResponse =
        when (operation) {
            "CreateServiceMetadataPublisherService", "CreateServiceMetadataPublisherServiceRequest", "CreateServiceMetadataPublisherServiceType", "Create" -> {
                createParticipant(requestXml)
            }

            "ReadServiceMetadataPublisherService", "Read" -> {
                handleRead(requestXml)
            }

            "UpdateServiceMetadataPublisherService", "Update" -> {
                handleUpdate(requestXml)
            }

            "DeleteServiceMetadataPublisherService", "Delete" -> {
                handleDelete(requestXml)
            }

            else -> {
                handleUnknown(operation)
            }
        }

    logger.debug("Wrapped response length={}", innerResponse.length)
    return wrapInSoapEnvelope(innerResponse)
}

private fun extractOperationFromSoapAction(soapAction: String?): String? {
    if (soapAction == null) return null
    val index = soapAction.lastIndexOf(':')
    if (index >= 0 && index < soapAction.length - 1) {
        return soapAction
            .substring(index + 1)
            .replace("In", "")
            .replace("In\"", "")
            .trim()
    }
    return soapAction
}

private fun extractOperationNameFromSoap(xml: String): String? {
    return try {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))

        // Find first element child of the SOAP Body
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) return n.localName ?: n.nodeName
            }
        }

        // Fallback: first non-envelope/header element
        val all = doc.getElementsByTagName("*")
        for (i in 0 until all.length) {
            val n = all.item(i)
            if (n is Element) {
                val local = n.localName ?: n.nodeName
                if (!local.contains("Envelope") && !local.contains("Header") && !local.contains("Body")) return local
            }
        }
        null
    } catch (e: Exception) {
        logger.debug("Failed to extract operation name from SOAP body: {}", e.message)
        null
    }
}

// Helper to get the first element child of the SOAP Body or null
private fun firstElementInSoapBody(xml: String): Element? {
    return try {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) return n
            }
        }
        null
    } catch (e: Exception) {
        logger.debug("Failed to parse SOAP body for first element: {}", e.message)
        null
    }
}

private fun preview(
    s: String?,
    max: Int = 512,
): String {
    if (s == null) return "null"
    return if (s.length <= max) s else s.substring(0, max) + "..."
}

private suspend fun Application.createParticipant(requestXml: String): String {
    logger.debug("createParticipant: entry, requestPreview={}", preview(requestXml))

    val opElement = firstElementInSoapBody(requestXml)
    if (opElement == null) {
        logger.debug("createParticipant: missing body element")
        val respPojo = CreateServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "missing body element")
        var xml = xmlMapper.writeValueAsString(respPojo)
        if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
        return xml
    }

    val opXml = nodeToString(opElement)
    logger.debug("createParticipant: opXmlPreview={}", preview(opXml))

    // Strictly rely on POJO binding
    val createPojo = xmlMapper.readValue(opXml, CreateServiceMetadataPublisherServiceRequestPojo::class.java)

    val model =
        ServiceMetadataPublisher(
            publisherId = createPojo.serviceMetadataPublisherID ?: generateTemporaryPublisherId(),
            logicalAddress = createPojo.publisherEndpoint?.logicalAddress ?: "",
            physicalAddress = createPojo.publisherEndpoint?.physicalAddress ?: "",
        )

    logger.debug(
        "createParticipant: creating model publisherId='{}' logical='{}' physical='{}'",
        model.publisherId,
        model.logicalAddress,
        model.physicalAddress,
    )

    val publisherService: PublisherService by dependencies
    val createdId = publisherService.create(model)

    val respPojo = CreateServiceMetadataPublisherServiceResponsePojo(result = "OK", databaseId = createdId)
    var xml = xmlMapper.writeValueAsString(respPojo)
    if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
    logger.debug("createParticipant: responding OK (length={})", xml.length)
    return xml
}

private fun Application.handleRead(requestXml: String): String {
    logger.debug("handleRead: entry, requestPreview={}", preview(requestXml))

    val opElement =
        firstElementInSoapBody(requestXml) ?: run {
            val resp = ReadServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "missing body element")
            var xml = xmlMapper.writeValueAsString(resp)
            if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
            return xml
        }

    val opXml = nodeToString(opElement)
    logger.debug("handleRead: opXmlPreview={}", preview(opXml))

    // Strictly use POJO binding
    val readPojo = xmlMapper.readValue(opXml, ReadServiceMetadataPublisherServiceRequestPojo::class.java)
    val publisherId = readPojo.serviceMetadataPublisherID

    if (publisherId.isNullOrBlank()) {
        val resp = ReadServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "Missing ServiceMetadataPublisherID")
        var xml = xmlMapper.writeValueAsString(resp)
        if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
        return xml
    }

    val publisherService: PublisherService by dependencies
    val found = publisherService.readByPublisherId(publisherId)
    if (found == null) {
        val resp = ReadServiceMetadataPublisherServiceResponsePojo(result = "OK")
        var xml = xmlMapper.writeValueAsString(resp)
        if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
        return xml
    }

    val respPojo =
        ReadServiceMetadataPublisherServiceResponsePojo(
            result = "OK",
            serviceMetadataPublisherID = found.publisherId,
            publisherEndpoint = PublisherEndpointPojo(found.logicalAddress, found.physicalAddress),
        )
    var xml = xmlMapper.writeValueAsString(respPojo)
    if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
    return xml
}

private fun Application.handleUpdate(requestXml: String): String {
    logger.debug("handleUpdate: entry, requestPreview={}", preview(requestXml))

    val opElement =
        firstElementInSoapBody(requestXml) ?: run {
            val resp = UpdateServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "missing body element")
            var xml = xmlMapper.writeValueAsString(resp)
            if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
            return xml
        }

    val opXml = nodeToString(opElement)
    logger.debug("handleUpdate: opXmlPreview={}", preview(opXml))

    val updatePojo = xmlMapper.readValue(opXml, UpdateServiceMetadataPublisherServiceRequestPojo::class.java)

    val publisherId = updatePojo.serviceMetadataPublisherID
    if (publisherId.isNullOrBlank()) {
        val resp = UpdateServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "Missing ServiceMetadataPublisherID")
        var xml = xmlMapper.writeValueAsString(resp)
        if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
        return xml
    }

    val model =
        ServiceMetadataPublisher(
            publisherId,
            updatePojo.publisherEndpoint?.logicalAddress ?: "",
            updatePojo.publisherEndpoint?.physicalAddress ?: "",
        )
    val publisherService: PublisherService by dependencies
    publisherService.update(model)

    val respPojo = UpdateServiceMetadataPublisherServiceResponsePojo(result = "OK")
    var xml = xmlMapper.writeValueAsString(respPojo)
    if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
    return xml
}

private fun Application.handleDelete(requestXml: String): String {
    logger.debug("handleDelete: entry, requestPreview={}", preview(requestXml))

    val opElement =
        firstElementInSoapBody(requestXml) ?: run {
            val resp = UpdateDeleteServiceResponsePojo(result = "Error", faultMessage = "missing body element")
            var xml = xmlMapper.writeValueAsString(resp)
            if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
            return xml
        }

    val opXml = nodeToString(opElement)
    logger.debug("handleDelete: opXmlPreview={}", preview(opXml))

    val deletePojo = xmlMapper.readValue(opXml, DeleteServiceMetadataPublisherServiceRequestPojo::class.java)
    val publisherId = deletePojo.serviceMetadataPublisherID
    if (publisherId.isNullOrBlank()) {
        val resp = UpdateDeleteServiceResponsePojo(result = "Error", faultMessage = "Missing ServiceMetadataPublisherID")
        var xml = xmlMapper.writeValueAsString(resp)
        if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
        return xml
    }

    val publisherService: PublisherService by dependencies
    publisherService.deleteByPublisherId(publisherId)

    val respPojo = UpdateDeleteServiceResponsePojo(result = "OK")
    var xml = xmlMapper.writeValueAsString(respPojo)
    if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
    return xml
}

private fun Application.handleUnknown(operation: String?): String {
    val op = operation ?: "Unknown"
    logger.debug("handleUnknown: operation='{}'", op)
    val respPojo = CreateServiceMetadataPublisherServiceResponsePojo(result = "Error", faultMessage = "Unsupported operation: $op")
    var xml = xmlMapper.writeValueAsString(respPojo)
    if (xml.startsWith("<?xml")) xml = xml.substring(xml.indexOf("?>") + 2)
    return xml
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

private fun generateTemporaryPublisherId(): String = "generated-" + System.currentTimeMillis().toString()
