package be.endevops

import be.endevops.svc.PublisherService
import be.endevops.xml.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.readValue


fun Application.configureManageServiceMetadata() {
    routing {
        get("/manage-service-metadata") {
            // Serve WSDL when '?wsdl' is present; otherwise return OK
            val wsdlRequested = call.request.queryParameters.contains("wsdl")
            if (!wsdlRequested) {
                call.respond(HttpStatusCode.OK)
            } else {
                val resource = this::class.java.classLoader.getResourceAsStream(
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
        post("/manage-service-metadata") {
            val raw = call.receiveText()
            val soapAction = call.request.header("SOAPAction")
            val responseXml = dispatchManageServiceMetadata(raw, soapAction)
            call.respondText(
                responseXml.map {
                    wrapInSoapEnvelope(it)
                }.getOrElse {
                    wrapInSoapEnvelope(it.message!!)
                },
                ContentType.Text.Xml.withCharset(Charsets.UTF_8),
                if (responseXml.isSuccess) HttpStatusCode.OK else HttpStatusCode.BadRequest
            )
        }
    }
}

// Dispatcher entry point
fun Application.dispatchManageServiceMetadata(
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
    val innerResponse = when (operation) {
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

class ManageServiceMetadata(val publisherService: PublisherService) {
    private val log = LoggerFactory.getLogger(ManageServiceMetadata::class.java)!!
    private val xmlMapper: XmlMapper = XmlMapper.builder().nameForTextElement("text").findAndAddModules().build()

    fun createParticipant(requestXml: String) = runCatching {
        log.debug("createParticipant: entry, requestPreview={}", requestXml)

        val opElement = firstElementInSoapBody(requestXml) ?: throw FaultError(
            xmlMapper.writeValueAsString(
                CreateServiceMetadataPublisherServiceResponsePojo(
                    result = "Error", faultMessage = "missing body element"
                ).also {
                    log.debug("createParticipant: missing body element")
                })
        )

        val opXml = nodeToString(opElement)
        log.debug("createParticipant: opXmlPreview={}", opXml)

        // Strictly rely on POJO binding
        val createPojo = xmlMapper.readValue<CreateServiceMetadataPublisherServiceRequestPojo>(opXml)
        val model = PublisherService.ServiceMetadataPublisher(
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
        xmlMapper.writeValueAsString(respPojo)
    }

    fun handleRead(requestXml: String) = runCatching {
        log.debug("handleRead: entry, requestPreview={}", requestXml)

        val opElement = firstElementInSoapBody(requestXml) ?: run {
            throw FaultError(
                xmlMapper.writeValueAsString(
                    ReadServiceMetadataPublisherServiceResponsePojo(
                        result = "Error", faultMessage = "missing body element"
                    )
                )
            )
        }

        val opXml = nodeToString(opElement)
        log.debug("handleRead: opXmlPreview={}", opXml)

        // Strictly use POJO binding
        val readPojo = xmlMapper.readValue<ReadServiceMetadataPublisherServiceRequestPojo>(opXml)

        log.info("handleRead: reading serviceMetadataPublisherID='{}'", readPojo.serviceMetadataPublisherID)
        val found = publisherService.get(readPojo.serviceMetadataPublisherID) ?: throw FaultError(
            xmlMapper.writeValueAsString(
                ReadServiceMetadataPublisherServiceResponsePojo(result = "OK")
            )
        )

        xmlMapper.writeValueAsString(
            ReadServiceMetadataPublisherServiceResponsePojo(
                result = "OK",
                serviceMetadataPublisherID = found.publisherId,
                publisherEndpoint = PublisherEndpointPojo(found.logicalAddress, found.physicalAddress),
            )
        )
    }

    fun handleUpdate(requestXml: String) = runCatching {
        log.debug("handleUpdate: entry, requestPreview={}", requestXml)

        val opElement = firstElementInSoapBody(requestXml) ?: throw FaultError(
            xmlMapper.writeValueAsString(
                UpdateServiceMetadataPublisherServiceResponsePojo(
                    result = "Error", faultMessage = "missing body element"
                )
            )
        )


        val opXml = nodeToString(opElement)
        log.debug("handleUpdate: opXmlPreview={}", opXml)

        val updatePojo = xmlMapper.readValue(opXml, UpdateServiceMetadataPublisherServiceRequestPojo::class.java)

        val model = PublisherService.ServiceMetadataPublisher(
            updatePojo.serviceMetadataPublisherID,
            updatePojo.publisherEndpoint.logicalAddress,
            updatePojo.publisherEndpoint.physicalAddress
        )

        publisherService.update(model)
        xmlMapper.writeValueAsString(UpdateServiceMetadataPublisherServiceResponsePojo(result = "OK"))
    }

    fun handleDelete(requestXml: String) = runCatching {
        log.debug("handleDelete: entry, requestPreview={}", requestXml)

        val opElement = firstElementInSoapBody(requestXml) ?: throw FaultError(
            xmlMapper.writeValueAsString(
                UpdateDeleteServiceResponsePojo(
                    result = "Error", faultMessage = "missing body element"
                )
            )
        )

        val opXml = nodeToString(opElement)
        log.debug("handleDelete: opXmlPreview={}", opXml)

        val deletePojo = xmlMapper.readValue(opXml, DeleteServiceMetadataPublisherServiceRequestPojo::class.java)
        publisherService.deleteByPublisherId(deletePojo.serviceMetadataPublisherID)

        xmlMapper.writeValueAsString(UpdateDeleteServiceResponsePojo(result = "OK"))
    }

    fun handleUnknown(operation: String?) = runCatching {
        val op = operation ?: "Unknown"
        log.debug("handleUnknown: operation='{}'", op)
        xmlMapper.writeValueAsString(
            CreateServiceMetadataPublisherServiceResponsePojo(
                result = "Error", faultMessage = "Unsupported operation: $op"
            )
        )
    }
}

