package be.endevops

import be.endevops.svc.*
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

fun Application.configureBusinessIdentifier() {
    routing {
        get("/manage-business-identifier") {
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
        post("/manage-business-identifier") {
            val raw = call.receiveText()
            val soapAction = call.request.header("SOAPAction")
            val responseXml = dispatchManageBusinessIdentifier(raw, soapAction)

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

fun Application.dispatchManageBusinessIdentifier(
    requestXml: String,
    soapActionHeader: String?,
): Result<String> {
    log.trace(
        "dispatchManageBusinessIdentifier called: soapAction='{}' requestLength={}",
        soapActionHeader,
        requestXml.length,
    )
    val operation = extractOperationNameFromSoap(requestXml) ?: extractOperationFromSoapAction(soapActionHeader)
    log.info("Determined operation='{}'", operation)
    log.debug("Request XML preview: {}", requestXml)

    val mbi: ManageBusinessIdentifier by dependencies
    val inner =
        when (operation) {
            "CreateParticipantIdentifier", "Create" -> {
                mbi.createParticipant(requestXml)
            }

            "DeleteParticipantIdentifier", "Delete" -> {
                mbi.deleteParticipant(requestXml)
            }

            "List", "ParticipantIdentifierPage", "PageRequest", "PageRequestType" -> {
                mbi.listParticipants(requestXml)
            }

            "CreateList", "CreateListIn", "CreateListType" -> {
                mbi.createListParticipants(requestXml)
            }

            "DeleteList", "DeleteListIn", "DeleteListType" -> {
                mbi.deleteParticipantList(requestXml)
            }

            "PrepareToMigrate", "PrepareMigrationRecord", "PrepareMigrationRecordType" -> {
                mbi.prepareToMigrate(requestXml)
            }

            "Migrate", "CompleteMigrationRecord", "CompleteMigrationRecordType" -> {
                mbi.migrate(requestXml)
            }

            else -> {
                log.warn("Unknown operation='{}'", operation)
                Result.failure(FaultError("<BadRequestFault><FaultMessage>Unknown operation: $operation</FaultMessage></BadRequestFault>"))
            }
        }

    return inner
}

class ManageBusinessIdentifier(
    val participantService: ParticipantService,
    val dnsClient: DnsClient,
    val publisherService: PublisherService,
    val migrationService: MigrationService
) {
    private val log = LoggerFactory.getLogger(ManageBusinessIdentifier::class.java)!!
    private val xmlMapper: XmlMapper = XmlMapper.builder().nameForTextElement("text").findAndAddModules().build()

    fun listParticipants(requestXml: String): Result<String> = runCatching {
        log.trace("listParticipants entry, preview={}", requestXml)
        val opElement =
            firstElementInSoapBody(requestXml)
                ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<PageRequestPojo>(opXml)

        log.info("Listing participant identifiers for publisher {}", req.serviceMetadataPublisherID)

        xmlMapper.writeValueAsString(participantService.listByPublisher(req.serviceMetadataPublisherID).map {
            ParticipantIdentifierPojo(it.scheme, it.identifier)
        }.let {
            PageResponsePojo(it)
        })
    }


    fun createParticipant(requestXml: String) = runCatching {
        log.trace("handleCreate entry, preview={}", requestXml)
        val opElement = firstElementInSoapBody(requestXml)
            ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<CreateParticipantIdentifierRequestPojo>(opXml)

        val smp = publisherService.get(req.serviceMetadataPublisherID)
            ?: throw FaultError("<BadRequestFault><FaultMessage>Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}</FaultMessage></BadRequestFault>")

        log.info(
            "Creating participant identifier for publisher {}: {}:{}",
            req.serviceMetadataPublisherID,
            req.participantIdentifier.scheme,
            req.participantIdentifier.identifier
        )
        createParticipantDns(smp, req.participantIdentifier.scheme, req.participantIdentifier.identifier)
        log.debug("DNS records created, inserting participant identifier in database")
        val dbId = participantService.create(
            ParticipantIdentifier(
                req.serviceMetadataPublisherID,
                req.participantIdentifier.scheme,
                req.participantIdentifier.identifier
            )
        )
        log.trace("handleCreate createdId={}", dbId)
        "<Result>OK</Result><DatabaseId>$dbId</DatabaseId>"
    }

    fun createListParticipants(requestXml: String) = runCatching {
        log.trace("createListParticipants entry, preview={}", requestXml)
        val opElement = firstElementInSoapBody(requestXml)
            ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<CreateListRequestPojo>(opXml)

        val smp = publisherService.get(req.serviceMetadataPublisherID)
            ?: throw FaultError("<BadRequestFault><FaultMessage>Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}</FaultMessage></BadRequestFault>")
        log.info("Creating participant identifiers for publisher {}", req.serviceMetadataPublisherID)
        if (req.participantIdentifier.isEmpty()) throw FaultError("<BadRequestFault><FaultMessage>No items to create</FaultMessage></BadRequestFault>")
        for (elem in req.participantIdentifier) {
            val scheme = elem.scheme
            val identifier = elem.identifier
            createParticipantDns(smp, elem.scheme, elem.identifier)
            participantService.create(ParticipantIdentifier(req.serviceMetadataPublisherID, scheme, identifier))
        }

        "<Result>OK</Result><Created>0</Created>"
    }

    private fun createParticipantDns(
        serviceMetadataPublisher: PublisherService.ServiceMetadataPublisher,
        scheme: String,
        identifier: String
    ) {
        val identifier = naptrIdentifierEncode(identifier)
        val cnameIdentifier = cnameIdentifierEncode(identifier)

        log.debug("Creating dns records")
        dnsClient.addNaptrRecord(
            "europa.eu",
            "${identifier}.${scheme}.acc.edelivery.tech.ec",
            30,
            20,
            10,
            "U",
            "Meta:SMP",
            "!^.*$!${serviceMetadataPublisher.logicalAddress}!",
            "."
        )
        dnsClient.addCNameRecord(
            "europa.eu",
            "B-${cnameIdentifier}.${scheme}.acc.edelivery.tech.ec",
            30,
            serviceMetadataPublisher.logicalAddress
        )
    }

    private fun deleteParticipantDns(
        scheme: String,
        identifier: String
    ) {
        val naptrId = naptrIdentifierEncode(identifier)
        val cnameId = cnameIdentifierEncode(identifier)

        log.debug("Deleting dns records")
        dnsClient.deleteNaptrRecord("europa.eu", "${naptrId}.${scheme}.acc.edelivery.tech.ec")
        dnsClient.deleteCNameRecord("europa.eu", "B-${cnameId}.${scheme}.acc.edelivery.tech.ec")
    }

    fun deleteParticipant(requestXml: String) = runCatching {
        log.trace("handleDelete entry, preview={}", requestXml)
        val opElement =
            firstElementInSoapBody(requestXml)
                ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<DeleteParticipantIdentifierRequestPojo>(opXml)

        if (!publisherService.exists(req.serviceMetadataPublisherID)) throw (FaultError("<BadRequestFault><FaultMessage>Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}</FaultMessage></BadRequestFault>"))
        log.info(
            "Deleting participant identifier for publisher {}: {}:{}",
            req.serviceMetadataPublisherID,
            req.participantIdentifier.scheme,
            req.participantIdentifier.identifier
        )
        deleteParticipantDns(req.participantIdentifier.scheme, req.participantIdentifier.identifier)
        val deleted = participantService.delete(
            req.serviceMetadataPublisherID, req.participantIdentifier.scheme,
            req.participantIdentifier.identifier
        )
        log.trace("handleDelete deleted={}", deleted)
        "<Result>OK</Result>"
    }

    fun deleteParticipantList(requestXml: String) = runCatching {
        log.trace("deleteParticipantList entry, preview={}", requestXml)
        val opElement =
            firstElementInSoapBody(requestXml)
                ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<DeleteListRequestPojo>(opXml)
        if (!publisherService.exists(req.serviceMetadataPublisherID)) throw FaultError("<BadRequestFault><FaultMessage>Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}</FaultMessage></BadRequestFault>")

        var deletedCount = 0
        for (elem in req.participantIdentifier) {
            val scheme = elem.scheme
            val identifier = elem.identifier
            deleteParticipantDns(scheme, identifier)
            log.info(
                "Deleting participant identifier for publisher {}: {}:{}",
                req.serviceMetadataPublisherID,
                scheme,
                identifier
            )
            if (participantService.delete(req.serviceMetadataPublisherID, scheme, identifier)) {
                deletedCount++
            }
        }

        "<Result>OK</Result><Deleted>$deletedCount</Deleted>"
    }

    fun prepareToMigrate(requestXml: String) = runCatching {
        log.trace("prepareToMigrate entry, preview={}", requestXml)
        val opElement =
            firstElementInSoapBody(requestXml)
                ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<PrepareMigrationRecordRequestPojo>(opXml)


        log.info(
            "Inserting new migration request from {} to {} for participant {}:{}", req.serviceMetadataPublisherID,
            req.toServiceMetadataPublisherID,
            req.participantIdentifier.scheme,
            req.participantIdentifier.identifier
        )
        val key = migrationService.create(
            MigrationService.MigrationRecord(
                req.migrationKey,
                req.serviceMetadataPublisherID,
                req.toServiceMetadataPublisherID,
                req.participantIdentifier.scheme,
                req.participantIdentifier.identifier
            )
        )

        "<Result>OK</Result><MigrationKey>${key}</MigrationKey>"
    }


    fun migrate(requestXml: String) = runCatching {
        log.trace("migrate entry, preview={}", requestXml)
        val opElement = firstElementInSoapBody(requestXml)
            ?: throw FaultError("<BadRequestFault><FaultMessage>missing body element</FaultMessage></BadRequestFault>")
        val opXml = nodeToString(opElement)
        val req = xmlMapper.readValue<CompleteMigrationRecordRequestPojo>(opXml)

        val migrationKey = req.migrationKey
        val rec = migrationService.get(migrationKey)
            ?: throw FaultError("<BadRequestFault><FaultMessage>Invalid migrate request</FaultMessage></BadRequestFault>")
        val smp = publisherService.get(rec.toPublisher)
            ?: throw FaultError("<BadRequestFault><FaultMessage>Unknown to ServiceMetadataPublisherID: ${rec.toPublisher}</FaultMessage></BadRequestFault>")

        log.info("Starting migration for key {}", req.migrationKey)
        log.debug("Updating DNS for participant {}:{}", rec.scheme, rec.identifier)
        updateParticipantDns(smp, rec.scheme, rec.identifier)
        log.debug("Updating participant records in database")
        participantService.delete(rec.fromPublisher, rec.scheme, rec.identifier)
        log.debug("Creating participant record for new publisher {}", rec.toPublisher)
        participantService.create(ParticipantIdentifier(rec.toPublisher, rec.scheme, rec.identifier))
        log.debug("Deleting migration record for key {}", migrationKey)
        migrationService.delete(migrationKey)
        log.info("Migration for key {} completed", req.migrationKey)
        "<Result>OK</Result>"
    }

    private fun updateParticipantDns(
        serviceMetadataPublisher: PublisherService.ServiceMetadataPublisher,
        scheme: String,
        identifier: String
    ) {
        val identifier = naptrIdentifierEncode(identifier)
        val cnameIdentifier = cnameIdentifierEncode(identifier)

        log.debug("Updating dns records")
        dnsClient.updateNaptrRecord(
            "europa.eu",
            "${identifier}.${scheme}.acc.edelivery.tech.ec",
            30,
            20,
            10,
            "U",
            "Meta:SMP",
            "!^.*$!${serviceMetadataPublisher.logicalAddress}",
            "."
        )
        dnsClient.updateCNameRecord(
            "europa.eu",
            "B-${cnameIdentifier}.${scheme}.acc.edelivery.tech.ec",
            30,
            serviceMetadataPublisher.logicalAddress
        )
    }

}

