package be.endevops

import be.endevops.xml.BadRequestFaultPojo
import be.endevops.xml.InternalErrorFaultPojo
import be.endevops.xml.NotFoundFaultPojo
import be.endevops.xml.Soap11Body
import be.endevops.xml.Soap11Envelope
import be.endevops.xml.toCompactFragment
import be.endevops.xml.toPlatformDocument
import io.ktor.http.HttpStatusCode
import nl.adaptivity.xmlutil.dom.PlatformDocument
import org.slf4j.Logger
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A recoverable request error. [detail] is the already serialized SOAP body content that is
 * returned to the caller, and [status] is the HTTP status mandated by the SML specification for
 * that kind of fault (see the eDelivery SML Interface Control Document, section 4.2.4).
 */
class FaultError(
    val detail: PlatformDocument,
    val status: HttpStatusCode,
    message: String? = null,
) : Throwable(message)

/** `badRequestFault` maps to HTTP 400 per the SML specification. */
fun badRequestFault(message: String): FaultError =
    FaultError(
        detail = APPLICATION_XML.toPlatformDocument(BadRequestFaultPojo(faultMessage = message)),
        status = HttpStatusCode.BadRequest,
        message = message,
    )

/** `notFoundFault` maps to HTTP 404 per the SML specification. */
fun notFoundFault(message: String): FaultError =
    FaultError(
        detail = APPLICATION_XML.toPlatformDocument(NotFoundFaultPojo(faultMessage = message)),
        status = HttpStatusCode.NotFound,
        message = message,
    )

/** `internalErrorFault` maps to HTTP 500 per the SML specification. */
fun internalErrorFault(message: String): FaultError =
    FaultError(
        detail = APPLICATION_XML.toPlatformDocument(InternalErrorFaultPojo(faultMessage = message)),
        status = HttpStatusCode.InternalServerError,
        message = message,
    )

/** The HTTP status and serializable body returned to the caller for a dispatch outcome. */
data class SoapResponse(
    val status: HttpStatusCode,
    val envelope: Soap11Envelope,
)

/**
 * Turns a dispatch result into the [SoapResponse] Ktor serializes.
 *
 * Both the success and failure paths return a serializable object so routes never build XML by
 * hand and never fall back to plain strings.
 */
fun Result<PlatformDocument>.toSoapResponse(logger: Logger): SoapResponse =
    fold(
        onSuccess = {
            SoapResponse(
                status = HttpStatusCode.OK,
                envelope = Soap11Envelope(body = Soap11Body(any = listOf(it.toCompactFragment()))),
            )
        },
        onFailure = { cause ->
            logger.error("Error processing request: {}", cause.message, cause.cause)
            val fault = cause as? FaultError ?: internalErrorFault(cause.message ?: "unknown error")
            SoapResponse(
                status = fault.status,
                envelope = Soap11Envelope(body = Soap11Body(any = listOf(fault.detail.toCompactFragment()))),
            )
        },
    )

fun firstElementInSoapBody(xml: String): Element? {
    return runCatching {
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
                if (n is Element) {
                    return n
                }
            }
        }
        null
    }.getOrNull()
}

fun extractOperationNameFromSoap(xml: String): String? {
    return runCatching {
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
                if (n is Element) {
                    return n.localName ?: n.nodeName
                }
            }
        }
        null
    }.getOrNull()
}

fun extractOperationFromSoapAction(soapAction: String?): String? {
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

fun nodeToString(node: Element): String {
    val sw =
        java.io
            .StringWriter()
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
