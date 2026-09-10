package be.endevops

import be.endevops.svc.DnsClient
import be.endevops.svc.MigrationService
import be.endevops.svc.ParticipantIdentifier
import be.endevops.svc.ParticipantService
import be.endevops.svc.PublisherService
import be.endevops.xml.CompleteMigrationRecordRequestPojo
import be.endevops.xml.CompleteMigrationRecordResponsePojo
import be.endevops.xml.CreateListRequestPojo
import be.endevops.xml.CreateListResponsePojo
import be.endevops.xml.CreateParticipantIdentifierRequestPojo
import be.endevops.xml.CreateParticipantIdentifierResponsePojo
import be.endevops.xml.DeleteListRequestPojo
import be.endevops.xml.DeleteListResponsePojo
import be.endevops.xml.DeleteParticipantIdentifierRequestPojo
import be.endevops.xml.DeleteParticipantIdentifierResponsePojo
import be.endevops.xml.MigrationRecordType
import be.endevops.xml.PageRequestPojo
import be.endevops.xml.PageResponsePojo
import be.endevops.xml.ParticipantIdentifierType
import be.endevops.xml.PrepareMigrationRecordResponsePojo
import be.endevops.xml.toPlatformDocument
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.di.annotations.Property
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.dom.PlatformDocument
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ManageBusinessIdentifier::class.java)!!

fun Application.configureBusinessIdentifier() {
    val getHandler: RoutingHandler = {
        val wsdlRequested =
            call.request.queryParameters
                .contains("wsdl")
        if (wsdlRequested) {
            val resource =
                this::class.java.classLoader
                    .getResourceAsStream("wsdl/peppol-sml-manage-business-identifier-service-v1.wsdl")
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
        } else {
            call.respond(HttpStatusCode.OK)
        }
    }
    val postHandler: RoutingHandler = {
        val raw = call.receiveText()
        val soapAction =
            call.request
                .header("SOAPAction")
        val response = dispatchManageBusinessIdentifier(raw, soapAction).toSoapResponse(logger)

        call.respond(response.status, response.envelope)
    }

    routing {
        get("/manageparticipantidentifier", getHandler)
        get("/manage-business-identifier", getHandler)
        post("/manageparticipantidentifier", postHandler)
        post("/manage-business-identifier", postHandler)
    }
}

suspend fun Application.dispatchManageBusinessIdentifier(
    requestXml: String,
    soapActionHeader: String?,
): Result<PlatformDocument> {
    log.trace("dispatchManageBusinessIdentifier called: soapAction='{}' requestLength={}", soapActionHeader, requestXml.length)
    val operation = extractOperationNameFromSoap(requestXml) ?: extractOperationFromSoapAction(soapActionHeader)
    log.info("Determined operation='{}'", operation)
    log.debug("Request XML preview: {}", requestXml)

    val mbi: ManageBusinessIdentifier by dependencies

    return when (operation) {
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
            Result.failure(badRequestFault("Unknown operation: $operation"))
        }
    }
}

class ManageBusinessIdentifier(
    val participantService: ParticipantService,
    val dnsClient: DnsClient,
    val publisherService: PublisherService,
    val migrationService: MigrationService,
    @Property("dns.zone")
    val zone: String,
    @Property("dns.domain")
    val domain: String,
) {
    private val log = LoggerFactory.getLogger(ManageBusinessIdentifier::class.java)!!

    suspend fun listParticipants(requestXml: String) =
        runCatching {
            log.trace("listParticipants entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            val opXml = nodeToString(opElement)
            val req = APPLICATION_XML.decodeFromString<PageRequestPojo>(opXml)

            log.info("Listing participant identifiers for publisher {}", req.serviceMetadataPublisherID)

            APPLICATION_XML.toPlatformDocument(
                participantService
                    .listByPublisher(req.serviceMetadataPublisherID)
                    .map {
                        ParticipantIdentifierType(it.identifier, it.scheme)
                    }.let {
                        PageResponsePojo(it)
                    },
            )
        }

    suspend fun createParticipant(requestXml: String) =
        runCatching {
            log.trace("handleCreate entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            val opXml = nodeToString(opElement)
            val req = APPLICATION_XML.decodeFromString<CreateParticipantIdentifierRequestPojo>(opXml)

            val smp =
                publisherService.get(req.serviceMetadataPublisherID)
                    ?: throw notFoundFault("Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}").also {
                        log.error("No publisher found for id {}", req.serviceMetadataPublisherID)
                    }

            log.info(
                "Creating participant identifier for publisher {}: {}:{}",
                req.serviceMetadataPublisherID,
                req.participantIdentifier.scheme,
                req.participantIdentifier.identifier,
            )
            createParticipantDns(smp, req.participantIdentifier.scheme, req.participantIdentifier.identifier)
            log.debug("DNS records created, inserting participant identifier in database")
            val dbId =
                participantService.create(
                    ParticipantIdentifier(
                        req.serviceMetadataPublisherID,
                        req.participantIdentifier.scheme,
                        req.participantIdentifier.identifier,
                    ),
                )
            log.trace("handleCreate createdId={}", dbId)
            APPLICATION_XML.toPlatformDocument(
                CreateParticipantIdentifierResponsePojo(result = "OK", databaseId = dbId),
            )
        }

    suspend fun createListParticipants(requestXml: String) =
        runCatching {
            log.trace("createListParticipants entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            val opXml = nodeToString(opElement)
            val req = APPLICATION_XML.decodeFromString<CreateListRequestPojo>(opXml)

            val smp =
                publisherService.get(req.serviceMetadataPublisherID)
                    ?: throw notFoundFault("Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}").also {
                        log.error("No publisher found for id {}", req.serviceMetadataPublisherID)
                    }
            log.info("Creating participant identifiers for publisher {}", req.serviceMetadataPublisherID)
            if (req.participantIdentifier
                    .isEmpty()
            ) {
                throw badRequestFault("No items to create")
            }
            for (elem in req.participantIdentifier) {
                val scheme = elem.scheme
                val identifier = elem.identifier
                createParticipantDns(smp, elem.scheme, elem.identifier)
                participantService.create(ParticipantIdentifier(req.serviceMetadataPublisherID, scheme, identifier))
            }

            APPLICATION_XML.toPlatformDocument(
                CreateListResponsePojo(result = "OK", created = 0),
            )
        }

    private fun createParticipantDns(
        serviceMetadataPublisher: PublisherService.ServiceMetadataPublisher,
        scheme: String,
        identifier: String,
    ) {
        val identifier = naptrIdentifierEncode(identifier)
        val cnameIdentifier = cnameIdentifierEncode(identifier)

        log.debug("Creating dns records")
        dnsClient.addNaptrRecord(
            zone,
            "$identifier.$scheme.$domain",
            30,
            20,
            10,
            "U",
            "Meta:SMP",
            "!^.*$!${serviceMetadataPublisher.logicalAddress}!",
            ".",
        )
        dnsClient.addCNameRecord(
            zone,
            "B-$cnameIdentifier.$scheme.$domain",
            30,
            serviceMetadataPublisher.logicalAddress,
        )
    }

    private fun deleteParticipantDns(
        scheme: String,
        identifier: String,
    ) {
        val naptrId = naptrIdentifierEncode(identifier)
        val cnameId = cnameIdentifierEncode(identifier)

        log.debug("Deleting dns records")
        dnsClient.deleteNaptrRecord(zone, "$naptrId.$scheme.$domain")
        dnsClient.deleteCNameRecord(zone, "B-$cnameId.$scheme.$domain")
    }

    suspend fun deleteParticipant(requestXml: String) =
        runCatching {
            log.trace("handleDelete entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            val opXml = nodeToString(opElement)
            val req = APPLICATION_XML.decodeFromString<DeleteParticipantIdentifierRequestPojo>(opXml)

            if (!publisherService.exists(req.serviceMetadataPublisherID)) {
                throw notFoundFault("Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}").also {
                    log.error("No publisher found for id {}", req.serviceMetadataPublisherID)
                }
            }
            log.info(
                "Deleting participant identifier for publisher {}: {}:{}",
                req.serviceMetadataPublisherID,
                req.participantIdentifier.scheme,
                req.participantIdentifier.identifier,
            )
            deleteParticipantDns(req.participantIdentifier.scheme, req.participantIdentifier.identifier)
            val deleted =
                participantService.delete(
                    req.serviceMetadataPublisherID,
                    req.participantIdentifier.scheme,
                    req.participantIdentifier.identifier,
                )
            log.trace("handleDelete deleted={}", deleted)
            APPLICATION_XML.toPlatformDocument(
                DeleteParticipantIdentifierResponsePojo(result = "OK"),
            )
        }

    suspend fun deleteParticipantList(requestXml: String) =
        runCatching {
            log.trace("deleteParticipantList entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            val opXml = nodeToString(opElement)
            val req = APPLICATION_XML.decodeFromString<DeleteListRequestPojo>(opXml)
            if (!publisherService.exists(req.serviceMetadataPublisherID)) {
                throw notFoundFault("Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}").also {
                    log.error("No publisher found for id {}", req.serviceMetadataPublisherID)
                }
            }

            var deletedCount = 0
            for (elem in req.participantIdentifier) {
                val scheme = elem.scheme
                val identifier = elem.identifier
                deleteParticipantDns(scheme, identifier)
                log.info(
                    "Deleting participant identifier for publisher {}: {}:{}",
                    req.serviceMetadataPublisherID,
                    scheme,
                    identifier,
                )
                if (participantService.delete(req.serviceMetadataPublisherID, scheme, identifier)) {
                    deletedCount++
                }
            }

            APPLICATION_XML.toPlatformDocument(
                DeleteListResponsePojo(result = "OK", deleted = deletedCount),
            )
        }

    suspend fun prepareToMigrate(requestXml: String) =
        runCatching {
            log.trace("prepareToMigrate entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }
            log.debug("Deserializing")
            val req =
                runCatching {
                    APPLICATION_XML.decodeFromString<MigrationRecordType>(nodeToString(opElement))
                }.onFailure {
                    log.error("Deserialization failed", it)
                }.getOrThrow()

            log.info(
                "Inserting new migration request from {} for participant {}:{}",
                req.serviceMetadataPublisherID,
                req.participantIdentifier.scheme,
                req.participantIdentifier.identifier,
            )
            val key =
                migrationService.create(
                    MigrationService.MigrationRecord(
                        req.migrationKey,
                        req.serviceMetadataPublisherID,
                        req.participantIdentifier.scheme,
                        req.participantIdentifier.identifier,
                    ),
                )

            APPLICATION_XML.toPlatformDocument(
                PrepareMigrationRecordResponsePojo(result = "OK", migrationKey = key),
            )
        }

    suspend fun migrate(requestXml: String) =
        runCatching {
            log.debug("migrate entry, preview={}", requestXml)
            val opElement =
                firstElementInSoapBody(requestXml)
                    ?: throw badRequestFault("missing body element").also {
                        log.error("Unable to parse the body element")
                    }

            val req =
                runCatching {
                    APPLICATION_XML.decodeFromString<CompleteMigrationRecordRequestPojo>(nodeToString(opElement))
                }.onFailure {
                    log.error("Deserialization failed", it)
                }.getOrThrow()

            val migrationKey = req.migrationKey
            log.info("Processing migration {}", req)
            val rec =
                migrationService.get(migrationKey)
                    ?: throw notFoundFault("Migration record not found: $migrationKey").also {
                        log.error("No migration record found for key {}", migrationKey)
                    }
            if (rec.identifier != req.participantIdentifier.identifier || rec.scheme != req.participantIdentifier.scheme) {
                throw badRequestFault("Invalid migrate request").also {
                    log.error(
                        "Migration record participant does not match request: record={} request={}",
                        rec,
                        req.participantIdentifier,
                    )
                }
            }

            log.info("Found migration record: {}", rec)
            val smp =
                publisherService.get(req.serviceMetadataPublisherID)
                    ?: throw notFoundFault("Unknown ServiceMetadataPublisherID: ${req.serviceMetadataPublisherID}").also {
                        log.error("No publisher found for id {}", req.serviceMetadataPublisherID)
                    }

            log.info("Starting migration for key {}", req.migrationKey)

            log.debug("Updating DNS for participant {}:{}", rec.scheme, rec.identifier)
            updateParticipantDns(smp, rec.scheme, rec.identifier)

            log.debug("Updating participant records in database")
            participantService.delete(rec.fromPublisher, rec.scheme, rec.identifier)

            log.debug("Creating participant record for new publisher {}", req.serviceMetadataPublisherID)
            participantService.create(ParticipantIdentifier(req.serviceMetadataPublisherID, rec.scheme, rec.identifier))

            log.debug("Deleting migration record for key {}", migrationKey)
            migrationService.delete(migrationKey)

            log.info("Migration for key {} completed", req.migrationKey)

            APPLICATION_XML.toPlatformDocument(
                CompleteMigrationRecordResponsePojo(result = "OK"),
            )
        }

    private fun updateParticipantDns(
        serviceMetadataPublisher: PublisherService.ServiceMetadataPublisher,
        scheme: String,
        identifier: String,
    ) {
        val identifier = naptrIdentifierEncode(identifier)
        val cnameIdentifier = cnameIdentifierEncode(identifier)

        log.debug("Updating dns records")
        dnsClient.updateNaptrRecord(
            zone,
            "$identifier.$scheme.$domain",
            30,
            20,
            10,
            "U",
            "Meta:SMP",
            "!^.*$!${serviceMetadataPublisher.logicalAddress}!",
            ".",
        )
        dnsClient.updateCNameRecord(
            zone,
            "B-$cnameIdentifier.$scheme.$domain",
            30,
            serviceMetadataPublisher.logicalAddress,
        )
    }
}
