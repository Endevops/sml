package be.endevops

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

// POJOs for Manage Business Identifier SOAP operations

data class ParticipantIdentifierPojo(
    @JacksonXmlProperty(localName = "scheme")
    @JsonAlias("Scheme")
    val scheme: String? = null,
    @JacksonXmlProperty(localName = "identifier")
    @JsonAlias("Identifier")
    val identifier: String? = null,
)

data class CreateParticipantIdentifierRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "ParticipantIdentifier")
    val participantIdentifier: ParticipantIdentifierPojo? = null,
)

data class DeleteParticipantIdentifierRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "ParticipantIdentifier")
    val participantIdentifier: ParticipantIdentifierPojo? = null,
)

data class PageRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
)

data class CreateListRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "ParticipantIdentifier")
    @JacksonXmlElementWrapper(useWrapping = false)
    val participantIdentifier: List<ParticipantIdentifierPojo>? = null,
)

data class DeleteListRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "ParticipantIdentifier")
    @JacksonXmlElementWrapper(useWrapping = false)
    val participantIdentifier: List<ParticipantIdentifierPojo>? = null,
)

data class PrepareMigrationRecordRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "ParticipantIdentifier")
    val participantIdentifier: ParticipantIdentifierPojo? = null,
    @JacksonXmlProperty(localName = "MigrationKey")
    val migrationKey: String? = null,
    @JacksonXmlProperty(localName = "ToServiceMetadataPublisherID")
    val toServiceMetadataPublisherID: String? = null,
)

data class CompleteMigrationRecordRequestPojo(
    @JacksonXmlProperty(localName = "MigrationKey")
    val migrationKey: String? = null,
)
