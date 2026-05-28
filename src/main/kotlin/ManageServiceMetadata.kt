package be.endevops

import be.endevops.svc.PublisherService
import be.endevops.xml.CreateServiceMetadataPublisherServiceResponsePojo
import be.endevops.xml.CreateServiceMetadataPublisherServiceType
import be.endevops.xml.DeleteServiceMetadataPublisherServiceRequestPojo
import be.endevops.xml.PublisherEndpointType
import be.endevops.xml.ReadServiceMetadataPublisherServiceRequestPojo
import be.endevops.xml.ReadServiceMetadataPublisherServiceResponsePojo
import be.endevops.xml.UpdateDeleteServiceResponsePojo
import be.endevops.xml.UpdateServiceMetadataPublisherServiceRequestPojo
import be.endevops.xml.UpdateServiceMetadataPublisherServiceResponsePojo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

fun Application.configureManageServiceMetadata() {
    val logger = LoggerFactory.getLogger(Application::class.java)!!
    val getHandler: RoutingHandler = {
        // Serve WSDL when '?wsdl' is present; otherwise return OK
        val wsdlRequested =
            call.request.queryParameters
                .contains("wsdl")
        if (!wsdlRequested) {
            call.respond(HttpStatusCode.OK)
        } else {
            val resource =
                this::class.java.classLoader.getResourceAsStream(
                    "wsdl/peppol-sml-manage-service-metadata-service-v1.wsdl",
                )
            if (resource != null) {
                val content = resource.readBytes()
                call.respondBytes(
                    content,
                    ContentType.Text.Xml
                        .withCharset(Charsets.UTF_8),
                    HttpStatusCode.OK,
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    val postHandler: RoutingHandler = {
        val raw = call.receiveText()
        val soapAction =
            call.request
                .header("SOAPAction")
        val responseXml = dispatchManageServiceMetadata(raw, soapAction)
        call.respondText(
            responseXml
                .map {
                    wrapInSoapEnvelope(it)
                }.getOrElse {
                    logger.error("Error processing request: {}", it.message, it.cause)
                    wrapInSoapEnvelope(it.message!!)
                },
            ContentType.Text.Xml
                .withCharset(Charsets.UTF_8),
            if (responseXml.isSuccess) HttpStatusCode.OK else HttpStatusCode.BadRequest,
        )
    }

    routing {
        get("/manageservicemetadata", getHandler)
        get("/manage-service-metadata", getHandler)
        post("/manageservicemetadata", postHandler)
        post("/manage-service-metadata", postHandler)
    }
}

// Dispatcher entry point
suspend fun Application.dispatchManageServiceMetadata(
    requestXml: String,
    soapActionHeader: String?,
): Result<String> {
    log.trace(
        "dispatchManageServiceMetadata called: soapActionHeader='{}', requestLength={}",
        soapActionHeader,
        requestXml.length,
    )
    val operation = extractOperationNameFromSoap(requestXml) ?: extractOperationFromSoapAction(soapActionHeader)
    log.info("Determined operation='{}'", operation)
    log.debug("Request XML preview: {}", requestXml)

    val manageServiceMetadata: ManageServiceMetadata by dependencies
    val innerResponse =
        when (operation) {
            "CreateServiceMetadataPublisherService", "CreateServiceMetadataPublisherServiceRequest", "CreateServiceMetadataPublisherServiceType", "Create" -> {
                manageServiceMetadata.createParticipant(requestXml)
            }

            "ReadServiceMetadataPublisherService", "Read" -> {
                manageServiceMetadata.handleRead(requestXml)
            }

            "UpdateServiceMetadataPublisherService", "Update" -> {
                manageServiceMetadata.handleUpdate(requestXml)
            }

            "DeleteServiceMetadataPublisherService", "Delete" -> {
                manageServiceMetadata.handleDelete(requestXml)
            }

            else -> {
                manageServiceMetadata.handleUnknown(operation)
            }
        }

    return innerResponse
}

class ManageServiceMetadata(
    val publisherService: PublisherService,
) {
    private val log = LoggerFactory.getLogger(ManageServiceMetadata::class.java)!!

    suspend fun createParticipant(requestXml: String) =
        runCatching {
            log.debug("createParticipant: entry, requestPreview={}", requestXml)

            val opElement =
                firstElementInSoapBody(requestXml) ?: throw FaultError(
                    APPLICATION_XML.encodeToString(
                        CreateServiceMetadataPublisherServiceResponsePojo(
                            result = "Error",
                            faultMessage = "missing body element",
                        ).also {
                            log.debug("createParticipant: missing body element")
                        },
                    ),
                )

            val opXml = nodeToString(opElement)
            log.debug("createParticipant: opXmlPreview={}", opXml)

            // Strictly rely on POJO binding
            val createPojo = APPLICATION_XML.decodeFromString<CreateServiceMetadataPublisherServiceType>(opXml)
            val model =
                PublisherService.ServiceMetadataPublisher(
                    publisherId = createPojo.serviceMetadataPublisherID,
                    logicalAddress = createPojo.publisherEndpoint.logicalAddress,
                    physicalAddress = createPojo.publisherEndpoint.physicalAddress,
                )

            log.info(
                "createParticipant: creating model publisherId='{}' logical='{}' physical='{}'",
                model.publisherId,
                model.logicalAddress,
                model.physicalAddress,
            )
            val createdId = publisherService.create(model)

            val respPojo = CreateServiceMetadataPublisherServiceResponsePojo(result = "OK", databaseId = createdId)
            APPLICATION_XML.encodeToString(respPojo)
        }

    suspend fun handleRead(requestXml: String) =
        runCatching {
            log.debug("handleRead: entry, requestPreview={}", requestXml)

            val opElement =
                firstElementInSoapBody(requestXml) ?: run {
                    throw FaultError(
                        APPLICATION_XML.encodeToString(
                            ReadServiceMetadataPublisherServiceResponsePojo(
                                result = "Error",
                                faultMessage = "missing body element",
                            ),
                        ),
                    )
                }

            val opXml = nodeToString(opElement)
            log.debug("handleRead: opXmlPreview={}", opXml)

            // Strictly use POJO binding
            val readPojo = APPLICATION_XML.decodeFromString<ReadServiceMetadataPublisherServiceRequestPojo>(opXml)

            log.info("handleRead: reading serviceMetadataPublisherID='{}'", readPojo.serviceMetadataPublisherID)
            val found =
                publisherService.get(readPojo.serviceMetadataPublisherID) ?: throw FaultError(
                    APPLICATION_XML.encodeToString(
                        ReadServiceMetadataPublisherServiceResponsePojo(
                            result = "NOT_FOUND",
                            faultMessage = "Publisher not found",
                        ),
                    ),
                )

            APPLICATION_XML.encodeToString(
                ReadServiceMetadataPublisherServiceResponsePojo(
                    result = "OK",
                    serviceMetadataPublisherID = found.publisherId,
                    publisherEndpoint = PublisherEndpointType(found.logicalAddress, found.physicalAddress),
                ),
            )
        }

    suspend fun handleUpdate(requestXml: String) =
        runCatching {
            log.debug("handleUpdate: entry, requestPreview={}", requestXml)

            val opElement =
                firstElementInSoapBody(requestXml) ?: throw FaultError(
                    APPLICATION_XML.encodeToString(
                        UpdateServiceMetadataPublisherServiceResponsePojo(
                            result = "Error",
                            faultMessage = "missing body element",
                        ),
                    ),
                )

            val opXml = nodeToString(opElement)
            log.debug("handleUpdate: opXmlPreview={}", opXml)

            val updatePojo = APPLICATION_XML.decodeFromString<UpdateServiceMetadataPublisherServiceRequestPojo>(opXml)

            val model =
                PublisherService.ServiceMetadataPublisher(
                    updatePojo.serviceMetadataPublisherID,
                    updatePojo.publisherEndpoint.logicalAddress,
                    updatePojo.publisherEndpoint.physicalAddress,
                )

            publisherService.update(model)
            APPLICATION_XML.encodeToString(UpdateServiceMetadataPublisherServiceResponsePojo(result = "OK"))
        }

    suspend fun handleDelete(requestXml: String) =
        runCatching {
            log.debug("handleDelete: entry, requestPreview={}", requestXml)

            val opElement =
                firstElementInSoapBody(requestXml) ?: throw FaultError(
                    APPLICATION_XML.encodeToString(
                        UpdateDeleteServiceResponsePojo(
                            result = "Error",
                            faultMessage = "missing body element",
                        ),
                    ),
                )

            val opXml = nodeToString(opElement)
            log.debug("handleDelete: opXmlPreview={}", opXml)

            val deletePojo = APPLICATION_XML.decodeFromString<DeleteServiceMetadataPublisherServiceRequestPojo>(opXml)
            publisherService.deleteByPublisherId(deletePojo.serviceMetadataPublisherID)

            APPLICATION_XML.encodeToString(UpdateDeleteServiceResponsePojo(result = "OK"))
        }

    fun handleUnknown(operation: String?) =
        runCatching {
            val op = operation ?: "Unknown"
            log.debug("handleUnknown: operation='{}'", op)
            APPLICATION_XML.encodeToString(
                CreateServiceMetadataPublisherServiceResponsePojo(
                    result = "Error",
                    faultMessage = "Unsupported operation: $op",
                ),
            )
        }
}
